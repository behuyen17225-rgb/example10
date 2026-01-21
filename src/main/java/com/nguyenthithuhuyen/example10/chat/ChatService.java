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
            // Bước 1: Dùng Gemini để phân tích intent, keyword, price từ message
            Map<String, Object> analysis = null;
            String intent = "UNKNOWN";
            String keyword = null;
            BigDecimal maxPrice = null;
            
            try {
                analysis = callGeminiForIntentAnalysis(message);
                if (analysis != null) {
                    intent = (String) analysis.getOrDefault("intent", "UNKNOWN");
                    keyword = (String) analysis.get("keyword");
                    Object priceObj = analysis.get("maxPrice");
                    if (priceObj != null) {
                        if (priceObj instanceof Number) {
                            maxPrice = new BigDecimal(((Number) priceObj).longValue());
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error analyzing intent: " + e.getMessage());
                // Fallback to keyword extraction
                keyword = extractKeyword(message);
                maxPrice = extractPrice(message);
            }

            // Nếu không có keyword từ Gemini, thử extract từ message
            if (keyword == null) {
                keyword = extractKeyword(message);
            }
            if (maxPrice == null) {
                maxPrice = extractPrice(message);
            }

            // ===== TRACK ORDER =====
            if ("TRACK_ORDER".equals(intent)) {
                response = ChatResponse.text("Bạn gửi giúp em mã đơn hàng để em kiểm tra nha 📦");
                response.setMessageType("TEXT");
            }
            // ===== FILTER BY PRICE =====
            else if ("FILTER_PRICE".equals(intent) && maxPrice != null) {
                List<ProductResponseDto> products = productRepo.searchByChat(
                    keyword, maxPrice, PageRequest.of(0, 5)
                ).stream().map(ProductMapper::toResponse).toList();
                
                if (products.isEmpty()) {
                    if (keyword != null) {
                        response = ChatResponse.text("Dạ hiện chưa có " + keyword + " dưới " + (maxPrice.longValue() / 1000) + "k 😥");
                    } else {
                        response = ChatResponse.text("Dạ hiện chưa có sản phẩm dưới " + (maxPrice.longValue() / 1000) + "k 😥");
                    }
                    response.setMessageType("TEXT");
                } else {
                    String msgText = "Em gợi ý sản phẩm dưới " + (maxPrice.longValue() / 1000) + "k cho bạn nè";
                    response = ChatResponse.products(msgText, products);
                    response.setMessageType("PRODUCT");
                }
            }
            // ===== SHOW PRODUCTS =====
            else if ("SHOW_PRODUCTS".equals(intent) && keyword != null) {
                List<ProductResponseDto> products = productRepo.searchByChat(
                    keyword, null, PageRequest.of(0, 5)
                ).stream().map(ProductMapper::toResponse).toList();
                
                if (products.isEmpty()) {
                    response = ChatResponse.text("Dạ hiện chưa có bánh " + keyword + " 😥");
                    response.setMessageType("TEXT");
                } else {
                    response = ChatResponse.products("Em gợi ý vài mẫu bánh cho bạn nè", products);
                    response.setMessageType("PRODUCT");
                }
            }
            // ===== GENERAL AI CHAT =====
            else {
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
                
                // Nếu QUOTA_EXCEEDED hoặc ERROR, không retry luôn return null
                if (result != null && (result.contains("QUOTA_EXCEEDED") || result.contains("GEMINI_ERROR"))) {
                    System.err.println("Gemini API quota exceeded or error: " + result);
                    return null;
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
     * Dùng Gemini để phân tích intent, keyword, maxPrice từ user message
     * Max 2 lần retry
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> callGeminiForIntentAnalysis(String message) {
        int maxRetries = 2;
        int delayMs = 500;
        
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                Map<String, Object> result = geminiService.askGeminiForIntent(message);
                
                // Check xem result có "ERROR" hoặc "QUOTA_EXCEEDED" không
                if (result != null && !result.isEmpty()) {
                    String intentVal = (String) result.getOrDefault("intent", "");
                    if (!"UNKNOWN".equals(intentVal) || (result.get("keyword") != null || result.get("maxPrice") != null)) {
                        return result;
                    }
                }
                
                if (attempt < maxRetries - 1) {
                    System.out.println("Intent analysis failed, retry " + (attempt + 1) + " after " + delayMs + "ms");
                    Thread.sleep(delayMs);
                    delayMs *= 2;
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Intent analysis interrupted: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Intent analysis attempt " + (attempt + 1) + " failed: " + e.getMessage());
                
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
        
        // Bánh / Cake
        keywordMap.put("b", "bánh");
        keywordMap.put("bnh", "bánh");
        keywordMap.put("bánh", "bánh");
        keywordMap.put("cake", "bánh");
        
        // Kem / Cream
        keywordMap.put("k", "kem");
        keywordMap.put("km", "kem");
        keywordMap.put("kem", "kem");
        keywordMap.put("cream", "kem");
        
        // Socola / Chocolate
        keywordMap.put("sc", "socola");
        keywordMap.put("sô cô la", "socola");
        keywordMap.put("chocolate", "socola");
        keywordMap.put("choco", "socola");
        keywordMap.put("socola", "socola");
        
        // Trứng / Egg
        keywordMap.put("tr", "trứng");
        keywordMap.put("tứ", "trứng");
        keywordMap.put("egg", "trứng");
        keywordMap.put("trứng", "trứng");
        
        // Dâu / Strawberry
        keywordMap.put("dau", "dâu");
        keywordMap.put("strawberry", "dâu");
        keywordMap.put("dâu", "dâu");
        
        // Matcha
        keywordMap.put("mt", "matcha");
        keywordMap.put("matcha", "matcha");
        
        // Vanilla
        keywordMap.put("va", "vanilla");
        keywordMap.put("vani", "vanilla");
        keywordMap.put("vanilla", "vanilla");
        
        // Caramel
        keywordMap.put("cr", "caramel");
        keywordMap.put("carame", "caramel");
        keywordMap.put("caramel", "caramel");
        
        // Tiramisu
        keywordMap.put("tm", "tiramisu");
        keywordMap.put("tirami", "tiramisu");
        keywordMap.put("tiramisu", "tiramisu");
        
        // Bơ / Butter
        keywordMap.put("bo", "bơ");
        keywordMap.put("butter", "bơ");
        keywordMap.put("bơ", "bơ");
        
        // Nho / Grape
        keywordMap.put("nh", "nho");
        keywordMap.put("grape", "nho");
        keywordMap.put("nho", "nho");
        
        // Mint
        keywordMap.put("bac", "mint");
        keywordMap.put("bạc hà", "mint");
        keywordMap.put("mint", "mint");
        
        // Toffee
        keywordMap.put("tf", "toffee");
        keywordMap.put("taffy", "toffee");
        keywordMap.put("toffee", "toffee");
        
        // Opera
        keywordMap.put("op", "opera");
        keywordMap.put("opera", "opera");
        
        // Black Forest
        keywordMap.put("bf", "black forest");
        keywordMap.put("black", "black forest");
        keywordMap.put("forest", "black forest");
        keywordMap.put("black forest", "black forest");
        
        // Lấy tất cả words từ message
        String[] words = msg.split("\\s+");
        
        for (String word : words) {
            word = word.replaceAll("[^a-z0-9đáàảãạăằắẳẵặâầấẩẫậèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵỻỽ]", "");
            
            if (keywordMap.containsKey(word)) {
                return keywordMap.get(word);
            }
        }
        
        // Nếu không có trong map, tìm trong list từ khóa thô
        String[] keywords = {"bánh", "kem", "socola", "trứng", "dâu", "matcha", "vanilla", 
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
     * Trích xuất giá từ câu hỏi (ví: "dưới 100k", "giá 200k", "sp 150k" → 100000, 200000, 150000)
     */
    private BigDecimal extractPrice(String message) {
        // Pattern: (số) k hoặc đ (tìm số trước k hoặc đ)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)\\s*[kđ]");
        java.util.regex.Matcher matcher = pattern.matcher(message.toLowerCase());
        
        if (matcher.find()) {
            long price = Long.parseLong(matcher.group(1)) * 1000;
            return new BigDecimal(price);
        }
        return null;
    }
}
