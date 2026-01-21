package com.nguyenthithuhuyen.example10.chat;

import com.nguyenthithuhuyen.example10.dto.ProductResponseDto;
import com.nguyenthithuhuyen.example10.entity.ChatMessage;
import com.nguyenthithuhuyen.example10.entity.User;
import com.nguyenthithuhuyen.example10.mapper.ProductMapper;
import com.nguyenthithuhuyen.example10.payload.response.ChatResponse;
import com.nguyenthithuhuyen.example10.repository.ChatMessageRepository;
import com.nguyenthithuhuyen.example10.repository.ProductRepository;
import com.nguyenthithuhuyen.example10.repository.UserRepository;
import com.nguyenthithuhuyen.example10.security.services.GeminiService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final GeminiService geminiService;
    private final ProductRepository productRepo;
    private final ChatMessageRepository chatMessageRepo;
    private final UserRepository userRepo;

    /**
     * Xử lý tin nhắn chat từ user và lưu lịch sử
     * Logic:
     * 1. Nếu user hỏi liên quan sản phẩm/đơn hàng (semantic check) → lấy dữ liệu từ DB
     * 2. Nếu không liên quan → trả lời thân thiện qua Gemini
     */
    public ChatResponse handleChat(String message, Long userId) {

        // Lấy user từ DB
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Lấy lịch sử chat gần đây để cung cấp context
        List<ChatMessage> conversationHistory = chatMessageRepo.findRecentMessages(userId, 5);
        
        ChatResponse response = null;

        try {
            // Bước 1: Check semantic - câu hỏi có liên quan đến sản phẩm/đơn hàng không?
            boolean isProductOrOrderRelated = false;
            try {
                // Try to check semantic với retry
                String semanticResult = callGeminiWithRetry(message, "", false);
                isProductOrOrderRelated = semanticResult != null && !semanticResult.isEmpty();
            } catch (Exception e) {
                // Nếu lỗi check semantic, coi như liên quan product
                System.err.println("Error checking semantic: " + e.getMessage());
                isProductOrOrderRelated = true;
            }

            // Nếu KHÔNG liên quan đến sản phẩm/đơn hàng → trả lời thân thiện
            if (!isProductOrOrderRelated) {
                try {
                    // Call Gemini với retry
                    String aiAnswer = callGeminiWithRetry(message, convertToString(conversationHistory), true);
                    
                    // Nếu retry fail hoặc return null, dùng fallback
                    if (aiAnswer == null || aiAnswer.isEmpty() || aiAnswer.contains("ERROR")) {
                        aiAnswer = "Em xin lỗi, tại thời điểm này em đang bận. Vui lòng thử lại sau nhé! 😊";
                    }
                    
                    response = ChatResponse.text(aiAnswer);
                    response.setMessageType("TEXT");
                } catch (Exception e) {
                    // Fallback khi Gemini fail hoàn toàn
                    System.err.println("Error calling Gemini: " + e.getMessage());
                    response = ChatResponse.text("Em xin lỗi, tại thời điểm này em đang bận. Vui lòng thử lại sau nhé! 😊");
                    response.setMessageType("TEXT");
                }
            } 
            // Nếu liên quan → xác định intent bằng keyword (tránh gọi Gemini quá nhiều)
            else {
                String lowerMsg = message.toLowerCase();
                ChatResponse response2 = null;
                
                // ===== TRACK ORDER =====
                if (lowerMsg.contains("track") || lowerMsg.contains("đơn hàng") || 
                    lowerMsg.contains("kiểm tra") || lowerMsg.contains("order") ||
                    lowerMsg.contains("mã đơn")) {
                    response2 = ChatResponse.text("Bạn gửi giúp em mã đơn hàng để em kiểm tra nha 📦");
                    response2.setMessageType("TEXT");
                }
                // ===== SHOW / FILTER PRODUCTS =====
                else if (lowerMsg.contains("dưới") || lowerMsg.contains("limit") || 
                         lowerMsg.contains("giá") || lowerMsg.contains("price")) {
                    // Có filter giá
                    String keyword = extractKeyword(message);
                    BigDecimal maxPrice = extractPrice(message);
                    
                    if (maxPrice != null || keyword != null) {
                        List<ProductResponseDto> products = productRepo.searchByChat(
                            keyword, maxPrice, PageRequest.of(0, 5)
                        ).stream().map(ProductMapper::toResponse).toList();
                        
                        if (products.isEmpty()) {
                            response2 = ChatResponse.text("Dạ hiện chưa có bánh phù hợp mức giá này 😥");
                            response2.setMessageType("TEXT");
                        } else {
                            response2 = ChatResponse.products("Em gợi ý vài mẫu bánh phù hợp cho bạn nè", products);
                            response2.setMessageType("PRODUCT");
                        }
                    }
                }
                // ===== DEFAULT PRODUCT SEARCH =====
                else {
                    String keyword = extractKeyword(message);
                    if (keyword != null) {
                        List<ProductResponseDto> products = productRepo.searchByChat(
                            keyword, null, PageRequest.of(0, 5)
                        ).stream().map(ProductMapper::toResponse).toList();
                        
                        if (products.isEmpty()) {
                            response2 = ChatResponse.text("Dạ hiện chưa có bánh " + keyword + " 😥");
                            response2.setMessageType("TEXT");
                        } else {
                            response2 = ChatResponse.products("Em gợi ý vài mẫu bánh cho bạn nè", products);
                            response2.setMessageType("PRODUCT");
                        }
                    }
                }
                
                if (response2 != null) {
                    response = response2;
                } else {
                    // Fallback: canned response
                    response = ChatResponse.text("Em có thể giúp bạn tìm bánh hoặc kiểm tra đơn hàng. Bạn muốn gì ạ? 😊");
                    response.setMessageType("TEXT");
                }
            }
        } catch (Exception e) {
            // Ultimate fallback nếu có lỗi không mong muốn
            System.err.println("Unexpected error in handleChat: " + e.getMessage());
            e.printStackTrace();
            response = ChatResponse.text("Em xin lỗi, có lỗi xảy ra. Vui lòng thử lại sau! 😊");
            response.setMessageType("TEXT");
        }

        // Đảm bảo response không null
        if (response == null) {
            response = ChatResponse.text("Em có thể giúp bạn tìm bánh hoặc kiểm tra đơn hàng. Bạn muốn gì ạ? 😊");
            response.setMessageType("TEXT");
        }

        // Lưu chat message vào DB
        ChatMessage chatMsg = ChatMessage.builder()
            .user(user)
            .userMessage(message)
            .aiResponse(response.getText())
            .messageType(response.getMessageType())
            .createdAt(LocalDateTime.now())
            .build();
        chatMessageRepo.save(chatMsg);

        return response;
    }

    /**
     * Lấy lịch sử chat của user
     */
    public List<ChatMessage> getChatHistory(Long userId) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        return chatMessageRepo.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Retry với exponential backoff cho API call
     * Max 3 lần, delay: 1s → 2s → 4s
     */
    private String callGeminiWithRetry(String message, String context, boolean isGeneral) {
        int maxRetries = 3;
        int delayMs = 1000;
        
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                String result;
                if (isGeneral) {
                    result = geminiService.askGeminiGeneral(message, context);
                } else {
                    result = geminiService.isProductOrOrderRelated(message) ? "yes" : "no";
                }
                
                // Nếu thành công, return luôn
                if (result != null && !result.isEmpty() && !result.contains("ERROR")) {
                    return result;
                }
                
                // Nếu là lần cuối cùng, không sleep
                if (attempt < maxRetries - 1) {
                    System.out.println("Gemini call failed, retry " + (attempt + 1) + " after " + delayMs + "ms");
                    Thread.sleep(delayMs);
                    delayMs *= 2; // Exponential backoff
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Retry interrupted: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Attempt " + (attempt + 1) + " failed: " + e.getMessage());
                
                // Nếu không phải lần cuối, sleep rồi retry
                if (attempt < maxRetries - 1) {
                    try {
                        Thread.sleep(delayMs);
                        delayMs *= 2;
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        
        // Nếu hết lần retry, return fallback
        return null;
    }

    /**
     * Xóa toàn bộ chat history của user
     */
    public void clearChatHistory(Long userId) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        chatMessageRepo.deleteByUser(user);
    }

    /**
     * Chuyển đổi lịch sử chat thành string để gửi cho AI
     */
    private String convertToString(List<ChatMessage> messages) {
        return messages.stream()
            .map(msg -> String.format("User: %s\nAI: %s", msg.getUserMessage(), msg.getAiResponse()))
            .collect(Collectors.joining("\n\n"));
    }

    /**
     * Trích xuất keyword sản phẩm từ câu hỏi
     * Xử lý: viết tắt, synonyms, variations
     */
    private String extractKeyword(String message) {
        String msg = normalizeText(message.toLowerCase());
        
        // Map: [viết tắt / slang] → keyword chuẩn
        java.util.Map<String, String> keywordMap = new java.util.HashMap<>();
        
        // Socola / Chocolate
        keywordMap.put("sc", "socola");
        keywordMap.put("sô cô la", "socola");
        keywordMap.put("chocolate", "socola");
        keywordMap.put("choco", "socola");
        
        // Trứng / Egg
        keywordMap.put("tr", "trứng");
        keywordMap.put("tứ", "trứng");
        keywordMap.put("egg", "trứng");
        
        // Kem / Cream
        keywordMap.put("km", "kem");
        keywordMap.put("cream", "kem");
        
        // Dâu / Strawberry
        keywordMap.put("dau", "dâu");
        keywordMap.put("strawberry", "dâu");
        
        // Matcha
        keywordMap.put("mt", "matcha");
        
        // Vanilla
        keywordMap.put("va", "vanilla");
        keywordMap.put("vani", "vanilla");
        
        // Caramel
        keywordMap.put("cr", "caramel");
        keywordMap.put("carame", "caramel");
        
        // Tiramisu
        keywordMap.put("tm", "tiramisu");
        keywordMap.put("tirami", "tiramisu");
        
        // Bơ / Butter
        keywordMap.put("b", "bơ");
        keywordMap.put("bo", "bơ");
        keywordMap.put("butter", "bơ");
        
        // Nho / Grape
        keywordMap.put("nh", "nho");
        keywordMap.put("grape", "nho");
        
        // Mint
        keywordMap.put("bac", "mint");
        keywordMap.put("bạc hà", "mint");
        
        // Toffee
        keywordMap.put("tf", "toffee");
        keywordMap.put("taffy", "toffee");
        
        // Opera
        keywordMap.put("op", "opera");
        
        // Black Forest
        keywordMap.put("bf", "black forest");
        keywordMap.put("black", "black forest");
        keywordMap.put("forest", "black forest");
        keywordMap.put("bạc hà", "mint");
        
        // Toffee
        keywordMap.put("tf", "toffee");
        keywordMap.put("taffy", "toffee");
        
        // Opera
        keywordMap.put("op", "opera");
        
        // Black Forest
        keywordMap.put("bf", "black forest");
        keywordMap.put("black", "black forest");
        keywordMap.put("forest", "black forest");
        
        // Lấy tất cả words từ message
        String[] words = msg.split("\\s+");
        
        for (String word : words) {
            word = word.replaceAll("[^a-z0-9đáàảãạăằắẳẵặâầấẩẫậèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵỻỽ]", "");
            
            if (keywordMap.containsKey(word)) {
                return keywordMap.get(word);
            }
        }
        
        // Nếu không có trong map, tìm trong list từ khóa thô
        String[] keywords = {"socola", "trứng", "kem", "dâu", "matcha", "vanilla", 
            "caramel", "toffee", "mint", "nho", "bơ", "tiramisu", "opera", "black"};
        
        for (String keyword : keywords) {
            if (msg.contains(keyword)) {
                return keyword;
            }
        }
        
        return null;
    }

    /**
     * Normalize text: loại bỏ dấu, chuyển thường
     */
    private String normalizeText(String text) {
        // Loại bỏ diacritics
        String normalized = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    /**
     * Trích xuất giá từ câu hỏi (ví: "dưới 100k" → 100000)
     */
    private BigDecimal extractPrice(String message) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)\\s*k");
        java.util.regex.Matcher matcher = pattern.matcher(message.toLowerCase());
        
        if (matcher.find()) {
            long price = Long.parseLong(matcher.group(1)) * 1000;
            return new BigDecimal(price);
        }
        return null;
    }
}
