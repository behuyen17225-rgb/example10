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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final GeminiService geminiService;
    private final ProductRepository productRepo;
    private final ChatMessageRepository chatMessageRepo;
    private final UserRepository userRepo;

    // Helper class để lưu price range
    private static class PriceRange {
        BigDecimal minPrice;
        BigDecimal maxPrice;

        PriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
            this.minPrice = minPrice;
            this.maxPrice = maxPrice;
        }
    }

    /**
     * Xử lý tin nhắn chat từ user và lưu lịch sử
     * Logic:
     * 1. Nếu user hỏi liên quan sản phẩm/đơn hàng (semantic check) → lấy dữ liệu từ
     * DB
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
            // Bước 1: Phân tích intent bằng keyword matching (KHÔNG gọi Gemini)
            String lowerMsg = message.toLowerCase();
            String intent = "UNKNOWN";
            String keyword = null;
            BigDecimal minPrice = null;
            BigDecimal maxPrice = null;

            // ===== DETECT INTENT =====
            if (lowerMsg.contains("track") || lowerMsg.contains("đơn hàng") ||
                    lowerMsg.contains("kiểm tra") || lowerMsg.contains("order") ||
                    lowerMsg.contains("mã đơn")) {
                intent = "TRACK_ORDER";
            } else if (lowerMsg.contains("dưới") || lowerMsg.contains("limit") ||
                    lowerMsg.contains("giá") || lowerMsg.contains("price") ||
                    lowerMsg.contains("sp") || lowerMsg.contains("sản phẩm") ||
                    lowerMsg.contains("trên") || lowerMsg.contains("từ")) {
                intent = "FILTER_PRICE";
                keyword = extractKeyword(message);
                // nếu không có keyword → tìm tất cả
                if (keyword == null)
                    keyword = "";
                PriceRange range = extractPriceRange(message);
                if (range != null) {
                    minPrice = range.minPrice;
                    maxPrice = range.maxPrice;
                }
            } else if (lowerMsg.contains("bánh") || lowerMsg.contains("kem") ||
                    lowerMsg.contains("socola") || lowerMsg.contains("trứng") ||
                    lowerMsg.contains("matcha") || lowerMsg.contains("vanilla")) {
                intent = "SHOW_PRODUCTS";
                keyword = extractKeyword(message);
            }

            // ===== TRACK ORDER =====
            if ("TRACK_ORDER".equals(intent)) {
                response = ChatResponse.text("Bạn gửi giúp em mã đơn hàng để em kiểm tra nha 📦");
                response.setMessageType("TEXT");
            }
            // ===== FILTER BY PRICE =====
            else if ("FILTER_PRICE".equals(intent) && (minPrice != null || maxPrice != null)) {

                List<ProductResponseDto> products = productRepo
                        .searchByChat(
                                keyword == null ? "" : keyword,
                                minPrice,
                                maxPrice,
                                PageRequest.of(0, 5))
                        .stream()
                        .map(ProductMapper::toResponse)
                        .toList();

                if (products.isEmpty()) {
                    String priceText = buildPriceRangeText(minPrice, maxPrice);
                    response = ChatResponse.text(
                            keyword != null
                                    ? "Dạ hiện chưa có " + keyword + " " + priceText + " 😥"
                                    : "Dạ hiện chưa có sản phẩm " + priceText + " 😥");
                    response.setMessageType("TEXT");
                } else {
                    response = ChatResponse.products(
                            buildSuggestionText(keyword, minPrice, maxPrice),
                            products);
                    response.setMessageType("PRODUCT");
                }
            } else if ("SHOW_PRODUCTS".equals(intent) && keyword != null) {
                List<ProductResponseDto> products = productRepo.searchByChat(
                        keyword, null, PageRequest.of(0, 5)).stream().map(ProductMapper::toResponse).toList();

                if (products.isEmpty()) {
                    response = ChatResponse.text("Dạ hiện chưa có bánh " + keyword + " 😥");
                    response.setMessageType("TEXT");
                } else {
                    response = ChatResponse.products("Em gợi ý vài mẫu bánh cho bạn nè", products);
                    response.setMessageType("PRODUCT");
                }
            }
            // ===== GENERAL AI CHAT (Chỉ gọi Gemini ở đây) =====
            else {
                try {
                    // Call Gemini với retry
                    String aiAnswer = callGeminiWithRetry(message, convertToString(conversationHistory), true);

                    // Nếu retry fail hoặc return null, dùng fallback
                    if (aiAnswer == null || aiAnswer.isEmpty() || aiAnswer.contains("ERROR")
                            || aiAnswer.contains("QUOTA_EXCEEDED")) {
                        aiAnswer = "Em xin lỗi, tại thời điểm này em đang bận. Vui lòng thử lại sau nhé! 😊";
                    }

                    response = ChatResponse.text(aiAnswer);
                    response.setMessageType("TEXT");
                } catch (Exception e) {
                    // Fallback khi Gemini fail hoàn toàn
                    System.err.println("Error calling Gemini: " + e.getMessage());
                    response = ChatResponse
                            .text("Em xin lỗi, tại thời điểm này em đang bận. Vui lòng thử lại sau nhé! 😊");
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
                    if (!"UNKNOWN".equals(intentVal)
                            || (result.get("keyword") != null || result.get("maxPrice") != null)) {
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
     * Xử lý: viết tắt, synonyms, variations, từ ghép
     */
    private String extractKeyword(String message) {
        String msg = normalizeText(message.toLowerCase());

        // Map: [viết tắt / slang] → keyword chuẩn
        Map<String, String> keywordMap = new HashMap<>();

        // ===== TỪ GHÉP (Kiểm tra trước) =====
        keywordMap.put("bánh kem", "bánh kem");
        keywordMap.put("bnh k", "bánh kem");
        keywordMap.put("bánh k", "bánh kem");
        keywordMap.put("b kem", "bánh kem");
        keywordMap.put("socola trứng", "socola trứng");
        keywordMap.put("sc tr", "socola trứng");

        // ===== TỪ ĐƠN =====
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

        // Kiểm tra từ ghép trước (đặt trước từ đơn)
        for (String pattern : new String[] { "bánh kem", "socola trứng", "bnh k", "bánh k", "b kem", "sc tr" }) {
            if (msg.contains(pattern)) {
                String result = keywordMap.get(pattern);
                if (result != null)
                    return result;
            }
        }

        // Lấy tất cả words từ message
        String[] words = msg.split("\\s+");

        for (String word : words) {
            word = word.replaceAll("[^a-z0-9đáàảãạăằắẳẵặâầấẩẫậèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵỻỽ]",
                    "");

            if (keywordMap.containsKey(word)) {
                return keywordMap.get(word);
            }
        }

        // Nếu không có trong map, tìm trong list từ khóa thô
        String[] keywords = { "bánh kem", "bánh", "kem", "socola", "trứng", "dâu", "matcha", "vanilla",
                "caramel", "toffee", "mint", "nho", "bơ", "tiramisu", "opera", "black" };

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
     * Trích xuất giá từ câu hỏi (ví: "dưới 100k", "giá 200k", "sp 150k" → 100000,
     * 200000, 150000)
     */
    private BigDecimal extractPrice(String message) {
        // Pattern: (số) k hoặc đ (tìm số trước k hoặc đ)
        Pattern pattern = Pattern.compile("(\\d+)\\s*[kđ]");
        Matcher matcher = pattern.matcher(message.toLowerCase());

        if (matcher.find()) {
            long price = Long.parseLong(matcher.group(1)) * 1000;
            return new BigDecimal(price);
        }
        return null;
    }

    /**
     * Trích xuất price range từ câu hỏi
     * Ví dụ:
     * "dưới 200k" → minPrice=null, maxPrice=200000
     * "trên 500k" → minPrice=500000, maxPrice=null
     * "từ 200k đến 500k" → minPrice=200000, maxPrice=500000
     */
    private PriceRange extractPriceRange(String message) {
        String msg = message.toLowerCase();
        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;

        // Pattern: "từ XXk đến YYk"
        Pattern fromToPattern = Pattern.compile("từ\\s+(\\d+)\\s*[kđ]\\s+đến\\s+(\\d+)\\s*[kđ]");
        Matcher fromToMatcher = fromToPattern.matcher(msg);
        if (fromToMatcher.find()) {
            minPrice = new BigDecimal(Long.parseLong(fromToMatcher.group(1)) * 1000);
            maxPrice = new BigDecimal(Long.parseLong(fromToMatcher.group(2)) * 1000);
            return new PriceRange(minPrice, maxPrice);
        }

        // Pattern: "dưới XXk"
        if (msg.contains("dưới")) {
            BigDecimal price = extractPrice(message);
            if (price != null) {
                return new PriceRange(null, price);
            }
        }

        // Pattern: "trên XXk"
        if (msg.contains("trên")) {
            BigDecimal price = extractPrice(message);
            if (price != null) {
                return new PriceRange(price, null);
            }
        }

        // Pattern: "giá XXk" hoặc "giá dưới XXk"
        if (msg.contains("giá")) {
            BigDecimal price = extractPrice(message);
            if (price != null) {
                if (msg.contains("dưới")) {
                    return new PriceRange(null, price);
                } else if (msg.contains("trên")) {
                    return new PriceRange(price, null);
                }
            }
        }

        return null;
    }

    /**
     * Build text mô tả price range
     */
    private String buildPriceRangeText(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null && maxPrice != null) {
            return "từ " + (minPrice.longValue() / 1000) + "k đến " + (maxPrice.longValue() / 1000) + "k";
        } else if (maxPrice != null) {
            return "dưới " + (maxPrice.longValue() / 1000) + "k";
        } else if (minPrice != null) {
            return "trên " + (minPrice.longValue() / 1000) + "k";
        }
        return "phù hợp";
    }

    /**
     * Build suggestion message text
     */
    private String buildSuggestionText(String keyword, BigDecimal minPrice, BigDecimal maxPrice) {
        StringBuilder sb = new StringBuilder("Em gợi ý ");

        if (keyword != null) {
            sb.append(keyword);
        } else {
            sb.append("sản phẩm");
        }

        if (minPrice != null && maxPrice != null) {
            sb.append(" từ ").append(minPrice.longValue() / 1000).append("k đến ").append(maxPrice.longValue() / 1000)
                    .append("k");
        } else if (maxPrice != null) {
            sb.append(" dưới ").append(maxPrice.longValue() / 1000).append("k");
        } else if (minPrice != null) {
            sb.append(" trên ").append(minPrice.longValue() / 1000).append("k");
        }

        sb.append(" cho bạn nè");
        return sb.toString();
    }
}
