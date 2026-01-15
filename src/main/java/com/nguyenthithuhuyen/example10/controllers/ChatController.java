package com.nguyenthithuhuyen.example10.controllers;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import com.nguyenthithuhuyen.example10.payload.request.ChatRequest;
import com.nguyenthithuhuyen.example10.payload.response.ChatResponse;
import com.nguyenthithuhuyen.example10.security.services.GeminiService;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final GeminiService geminiService;

    @PostMapping
    public Map<String, String> chat(@RequestBody Map<String, String> body) {
        return Map.of(
            "reply",
            geminiService.chat(body.get("message"))
        );
    }
}
