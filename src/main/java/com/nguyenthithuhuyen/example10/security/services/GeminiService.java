package com.nguyenthithuhuyen.example10.security.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.*;

@Service
public class GeminiService {

    @Value("${gemini.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String chat(String message) {

        String url =
          "https://generativelanguage.googleapis.com/v1beta/models/" +
          "gemini-1.5-flash:generateContent";

        // Body
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

        // Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);

        HttpEntity<Map<String, Object>> entity =
            new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response =
                restTemplate.postForEntity(url, entity, Map.class);

            Map<String, Object> responseBody = response.getBody();

            List<Map> candidates =
                (List<Map>) responseBody.get("candidates");

            Map content =
                (Map) candidates.get(0).get("content");

            List<Map> parts =
                (List<Map>) content.get("parts");

            return parts.get(0).get("text").toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "Gemini lỗi ❌";
        }
    }
}
