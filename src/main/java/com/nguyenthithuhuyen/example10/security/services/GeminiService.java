package com.nguyenthithuhuyen.example10.security.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.*;

@Service
public class GeminiService {

    @Value("${gemini.base-url}")
    private String baseUrl;

    @Value("${gemini.model}")
    private String model;

    @Value("${gemini.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String chat(String message) {
        try {
            // ✅ URL chuẩn Gemini
            String url = baseUrl +
                    "/v1beta/models/" + model + ":generateContent?key=" + apiKey;

            // ✅ Request body
            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of(
                                    "role", "user",
                                    "parts", List.of(
                                            Map.of("text", message)
                                    )
                            )
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(body, headers);

            ResponseEntity<Map> response =
                    restTemplate.postForEntity(url, entity, Map.class);

            Map<String, Object> responseBody =
                    (Map<String, Object>) response.getBody();

            List<Map<String, Object>> candidates =
                    (List<Map<String, Object>>) responseBody.get("candidates");

            Map<String, Object> content =
                    (Map<String, Object>) candidates.get(0).get("content");

            List<Map<String, Object>> parts =
                    (List<Map<String, Object>>) content.get("parts");

            return parts.get(0).get("text").toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "Gemini lỗi ❌";
        }
    }
}
