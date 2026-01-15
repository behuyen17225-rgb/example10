package com.nguyenthithuhuyen.example10.controllers;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*") // Quan trọng để React Native gọi được
public class ChatController {

    @Value("${gemini.api.key}")
    private String apiKey;

    // Sử dụng model ổn định nhất cho báo cáo
    private final String MODEL_NAME = "gemini-2.5-flash"; 
    private final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    @PostMapping
    public ResponseEntity<?> getChatResponse(@RequestBody Map<String, String> request) {
        String userPrompt = request.get("prompt");
        
        // Kiểm tra đầu vào
        if (userPrompt == null || userPrompt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Prompt không được để trống"));
        }

        RestTemplate restTemplate = new RestTemplate();
        String fullUrl = BASE_URL + MODEL_NAME + ":generateContent?key=" + apiKey;

        // 1. Tạo Header
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 2. Cấu trúc JSON body (Đúng chuẩn mảng lồng mảng của Google)
        Map<String, Object> body = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(
                    Map.of("text", userPrompt)
                ))
            )
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            // 3. Gọi API bằng PostForEntity để dễ xử lý response
            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(fullUrl, entity, Map.class);
            Map response = responseEntity.getBody();

            // 4. Bóc tách dữ liệu an toàn (tránh lỗi NullPointerException)
            if (response != null && response.containsKey("candidates")) {
                List candidates = (List) response.get("candidates");
                if (!candidates.isEmpty()) {
                    Map firstCandidate = (Map) candidates.get(0);
                    Map content = (Map) firstCandidate.get("content");
                    List parts = (List) content.get("parts");
                    Map firstPart = (Map) parts.get(0);
                    String aiText = (String) firstPart.get("text");

                    return ResponseEntity.ok(Map.of("text", aiText));
                }
            }
            
            return ResponseEntity.status(500).body(Map.of("error", "AI không có phản hồi phù hợp."));

        } catch (Exception e) {
            // In ra console để bạn dễ debug trên Railway
            System.err.println("Lỗi gọi Gemini: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi hệ thống: " + e.getMessage()));
        }
    }
}