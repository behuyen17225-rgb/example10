package com.nguyenthithuhuyen.example10.security.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Cache cho responses (tránh gọi Gemini lại cho câu hỏi giống nhau)
    private final Map<String, String> responseCache = new ConcurrentHashMap<>();
    private static final int CACHE_SIZE_LIMIT = 100;
    private static final long CACHE_TTL_MS = 3600000; // 1 giờ
    
    // Rate limiting - tối đa 15 requests/phút (để an toàn với free tier)
    private static final long MIN_REQUEST_INTERVAL_MS = 4000; // 4 giây giữa các request
    private long lastRequestTime = 0;
    private final Object rateLimitLock = new Object();
    
    // Cache entry với timestamp
    private static class CacheEntry {
        String value;
        long timestamp;
        CacheEntry(String value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }
    
    private final Map<String, CacheEntry> responseCacheWithTTL = new ConcurrentHashMap<>();
    
    /**
     * Enforce rate limiting - đảm bảo không gọi quá nhanh
     */
    private void enforceRateLimit() throws InterruptedException {
        synchronized (rateLimitLock) {
            long now = System.currentTimeMillis();
            long timeSinceLastRequest = now - lastRequestTime;
            
            if (timeSinceLastRequest < MIN_REQUEST_INTERVAL_MS) {
                long waitTime = MIN_REQUEST_INTERVAL_MS - timeSinceLastRequest;
                System.out.println("⏳ Rate limit: waiting " + waitTime + "ms before next request...");
                Thread.sleep(waitTime);
            }
            
            lastRequestTime = System.currentTimeMillis();
        }
    }

    /* ================= GỌI GEMINI – TEXT (WITH CACHING & RATE LIMITING) ================= */
    public String askGemini(String prompt) {
        // Kiểm tra cache trước
        String cacheKey = "gemini:" + prompt.hashCode();
        
        CacheEntry cached = responseCacheWithTTL.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            System.out.println("✅ Cache HIT: " + cacheKey);
            return cached.value;
        }

        try {
            // Enforce rate limiting
            enforceRateLimit();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "RATE_LIMITED";
        }

        String url =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-8b:generateContent"
            + "?key=" + apiKey;

        Map<String, Object> body = Map.of(
            "contents", List.of(
                Map.of(
                    "parts", List.of(
                        Map.of("text", prompt)
                    )
                )
            )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity =
            new HttpEntity<>(body, headers);

        try {
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> res =
                (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) restTemplate.postForEntity(url, entity, Map.class);

            @SuppressWarnings("unchecked")
            Map<String, Object> candidate =
                ((List<Map<String, Object>>) res.getBody()
                    .get("candidates")).get(0);

            @SuppressWarnings("unchecked")
            Map<String, Object> content =
                (Map<String, Object>) candidate.get("content");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> parts =
                (List<Map<String, Object>>) content.get("parts");

            String result = parts.get(0).get("text").toString();
            
            // Lưu vào cache với TTL
            if (responseCacheWithTTL.size() >= CACHE_SIZE_LIMIT) {
                responseCacheWithTTL.clear();
            }
            responseCacheWithTTL.put(cacheKey, new CacheEntry(result));
            
            return result;

        } catch (HttpClientErrorException.TooManyRequests e) {
            System.err.println("❌ Gemini API Quota Exceeded (429): " + e.getMessage());
            return "QUOTA_EXCEEDED";
            
        } catch (Exception e) {
            System.err.println("❌ Gemini API Error: " + e.getMessage());
            return "GEMINI_ERROR";
        }
    }

    /* ================= GỌI GEMINI – GENERAL AI CHAT ================= */
    public String askGeminiGeneral(String userMessage, String conversationHistory) {

        String systemPrompt = """
        Bạn là một trợ lý AI thân thiện cho quán bánh. 
        Bạn có thể:
        - Giới thiệu sản phẩm bánh
        - Trả lời câu hỏi về đặt hàng
        - Giúp khách hàng tìm bánh phù hợp
        - Trò chuyện thân thiện và hỗ trợ khách hàng
        
        Hãy trả lời ngắn gọn, thân thiện, bằng tiếng Việt.
        """;

        String prompt = systemPrompt;
        
        if (conversationHistory != null && !conversationHistory.isEmpty()) {
            prompt += "\n\nLịch sử trò chuyện:\n" + conversationHistory;
        }
        
        prompt += "\n\nKhách hàng: " + userMessage + "\nBạn:";

        return askGemini(prompt);
    }

    /* ================= GỌI GEMINI – INTENT ================= */
    public Map<String, Object> askGeminiForIntent(String userMessage) {

        String systemPrompt = """
        Bạn là trợ lý AI cho quán bánh.
        Phân tích câu người dùng và trả về JSON duy nhất.
        
        RULES:
        - Nếu user chỉ chào hỏi (hi, hello, xin chào, etc.) => intent: "UNKNOWN"
        - Nếu user hỏi về sản phẩm/tìm bánh => intent: "SHOW_PRODUCTS" (nếu không có filter giá)
        - Nếu user hỏi với điều kiện giá => intent: "FILTER_PRICE"
        - Nếu user track đơn hàng => intent: "TRACK_ORDER"
        - Các câu hỏi khác => intent: "UNKNOWN"
        - KHÔNG được gán SHOW_PRODUCTS nếu không có keyword hoặc maxPrice

        FORMAT:
        {
          "intent": "SHOW_PRODUCTS | FILTER_PRICE | TRACK_ORDER | UNKNOWN",
          "keyword": null hoặc string,
          "maxPrice": null hoặc number
        }

        Ví dụ:
        "hi" => {"intent":"UNKNOWN","keyword":null,"maxPrice":null}
        "bánh gì có" => {"intent":"UNKNOWN","keyword":null,"maxPrice":null}
        "bánh socola" => {"intent":"SHOW_PRODUCTS","keyword":"socola","maxPrice":null}
        "bánh socola dưới 100k" => {"intent":"FILTER_PRICE","keyword":"socola","maxPrice":100000}
        "track đơn ABC123" => {"intent":"TRACK_ORDER","keyword":"ABC123","maxPrice":null}
        """;

        String finalPrompt = systemPrompt + "\nUser: " + userMessage;

        String raw = askGemini(finalPrompt);

        return parseJsonSafe(raw);
    }

    /* ================= PARSE JSON ================= */
    private Map<String, Object> parseJsonSafe(String text) {
        try {
            String json =
                text.substring(text.indexOf("{"), text.lastIndexOf("}") + 1);

            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(json, Map.class);
            return result;
        } catch (Exception e) {
            return Map.of("intent", "UNKNOWN");
        }
    }

    /* ================= DETECT: Product or Order Related (Simple Keyword Check) ================= */
    /**
     * Kiểm tra xem câu hỏi có liên quan đến sản phẩm hoặc đơn hàng không
     * Dùng keyword matching thay vì gọi Gemini để tránh API overhead
     */
    public boolean isProductOrOrderRelated(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return false;
        }

        String msg = userMessage.toLowerCase();

        // Keywords liên quan đến sản phẩm/bánh
        String[] productKeywords = {
            "bánh", "bread", "cake", "product", "sản phẩm", "giá", "price", 
            "loại", "type", "hương vị", "flavor", "vị", "socola", "chocolate",
            "trứng", "egg", "cream", "kem", "bơ", "butter", "dâu", "strawberry",
            "nho", "grape", "matcha", "vanilla", "caramel", "toffee", "mint",
            "mua", "buy", "order bánh", "gợi ý", "recommend", "suggest",
            "bao nhiêu", "bao lâu", "mấy", "số", "cái", "chiếc", "hộp"
        };

        // Keywords liên quan đến đơn hàng
        String[] orderKeywords = {
            "đơn hàng", "order", "track", "kiểm tra", "tình trạng", "status",
            "giao hàng", "delivery", "ship", "mã đơn", "order id", "invoice",
            "thanh toán", "payment", "trả tiền", "tổng tiền", "bill"
        };

        // Check product keywords
        for (String keyword : productKeywords) {
            if (msg.contains(keyword)) {
                return true;
            }
        }

        // Check order keywords
        for (String keyword : orderKeywords) {
            if (msg.contains(keyword)) {
                return true;
            }
        }

        return false;
    }
}