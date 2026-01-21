package com.nguyenthithuhuyen.example10.controllers;

import com.nguyenthithuhuyen.example10.chat.ChatService;
import com.nguyenthithuhuyen.example10.dto.ChatMessageDto;
import com.nguyenthithuhuyen.example10.entity.ChatMessage;
import com.nguyenthithuhuyen.example10.payload.request.ChatRequest;
import com.nguyenthithuhuyen.example10.payload.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin
public class ChatController {

    private final ChatService chatService;

    /**
     * Gửi tin nhắn và nhận response từ AI
     */
    @PostMapping("/message")
    public ResponseEntity<ChatResponse> sendMessage(@RequestBody ChatRequest req) {
        if (req.getUserId() == null) {
            return ResponseEntity.badRequest().build();
        }
        
        ChatResponse response = chatService.handleChat(req.getPrompt(), req.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy lịch sử chat của user
     */


    /**
     * Endpoint cũ - giữ lại để compatibility (sử dụng userId = 1)
     */
    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest req) {
        if (req.getUserId() == null) {
            req.setUserId(1L); // Default user
        }
        return chatService.handleChat(req.getPrompt(), req.getUserId());
    }
}
