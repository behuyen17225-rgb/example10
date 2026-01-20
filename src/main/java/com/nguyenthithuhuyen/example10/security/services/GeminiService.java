package com.nguyenthithuhuyen.example10.security.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /* ================= GỌI GEMINI – TEXT ================= */
    public String askGemini(String prompt) {

        String url =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
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

            return parts.get(0).get("text").toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "Gemini error";
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

    /* ================= DETECT: Product or Order Related ================= */
    /**
     * Kiểm tra xem câu hỏi có liên quan đến sản phẩm hoặc đơn hàng không
     * Trả về true nếu có, false nếu chỉ là chat chung
     */
    public boolean isProductOrOrderRelated(String userMessage) {
        String systemPrompt = """
        Bạn là AI phân tích. Hãy kiểm tra câu user có liên quan đến:
        - Sản phẩm bánh (tìm kiếm, giá, loại, hương vị, v.v...)
        - Đơn hàng (tracking, tình trạng, giao hàng)
        
        Trả về JSON:
        {"related": true hoặc false}
        
        Ví dụ:
        "hi" => {"related":false}
        "bánh gì ngon?" => {"related":true}
        "xôi lên" => {"related":false}
        "bánh nào dưới 100k?" => {"related":true}
        "kiểm tra đơn hàng" => {"related":true}
        """;

        String finalPrompt = systemPrompt + "\nUser: " + userMessage;
        String raw = askGemini(finalPrompt);

        try {
            String json = raw.substring(raw.indexOf("{"), raw.lastIndexOf("}") + 1);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(json, Map.class);
            Object related = result.get("related");
            return related instanceof Boolean ? (Boolean) related : false;
        } catch (Exception e) {
            return false;
        }
    }
}