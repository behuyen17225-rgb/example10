package com.nguyenthithuhuyen.example10.controllers;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import com.nguyenthithuhuyen.example10.payload.request.ChatRequest;
import com.nguyenthithuhuyen.example10.payload.response.ChatResponse;
import com.nguyenthithuhuyen.example10.security.services.GeminiService;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin
public class ChatController {

    private final GeminiService geminiService;

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String reply = geminiService.chat(request.getMessage());
        return new ChatResponse(reply);
    }
}
