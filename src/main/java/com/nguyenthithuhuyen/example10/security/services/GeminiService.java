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

    /* ================= GỌI GEMINI – INTENT ================= */
    public Map<String, Object> askGeminiForIntent(String userMessage) {

        String systemPrompt = """
        Bạn là trợ lý AI cho quán bánh.
        Phân tích câu người dùng và trả về JSON duy nhất.

        FORMAT:
        {
          "intent": "SHOW_PRODUCTS | FILTER_PRICE | FILTER_FLAVOR | TRACK_ORDER",
          "keyword": "",
          "maxPrice": number | null
        }

        Ví dụ:
        bánh socola dưới 100k
        => {"intent":"FILTER_PRICE","keyword":"socola","maxPrice":100000}
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
}
