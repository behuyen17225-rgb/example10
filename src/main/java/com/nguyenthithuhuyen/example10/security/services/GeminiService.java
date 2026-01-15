package com.nguyenthithuhuyen.example10.security.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import lombok.RequiredArgsConstructor;
import jakarta.annotation.PostConstruct;

import java.util.*;

@Service
@RequiredArgsConstructor
public class GeminiService {

    @Value("${gemini.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String askGemini(String prompt) {
        String url =
            "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash:generateContent?key="
            + apiKey;

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

        HttpEntity<?> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> res =
                restTemplate.postForEntity(url, entity, Map.class);

            var candidates = (List<?>) res.getBody().get("candidates");
            if (candidates == null || candidates.isEmpty()) return "AI không trả lời";

            var content = (Map<?, ?>) ((Map<?, ?>) candidates.get(0)).get("content");
            var parts = (List<?>) content.get("parts");

            return parts.get(0).toString().replace("{text=", "").replace("}", "");
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Gemini lỗi";
        }
    }
}
