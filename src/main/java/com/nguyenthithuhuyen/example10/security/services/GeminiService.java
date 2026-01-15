package com.nguyenthithuhuyen.example10.security.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import lombok.RequiredArgsConstructor;
import jakarta.annotation.PostConstruct;

import java.util.*;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String askGemini(String prompt) {

        String url =
            "https://generativelanguage.googleapis.com/v1/models/gemini-1.0-pro:generateContent"
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
            ResponseEntity<Map> res = restTemplate.postForEntity(
                url, entity, Map.class
            );

            Map<String, Object> candidate =
                ((List<Map<String, Object>>) res.getBody().get("candidates")).get(0);

            Map<String, Object> content =
                (Map<String, Object>) candidate.get("content");

            List<Map<String, Object>> parts =
                (List<Map<String, Object>>) content.get("parts");

            return parts.get(0).get("text").toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "Gemini error";
        }
    }
}
