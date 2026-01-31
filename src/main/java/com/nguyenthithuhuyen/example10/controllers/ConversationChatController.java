package com.nguyenthithuhuyen.example10.controllers;

import com.nguyenthithuhuyen.example10.security.services.SecurityChatService;
import com.nguyenthithuhuyen.example10.payload.request.ChatRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@CrossOrigin
public class ConversationChatController {

    private final SecurityChatService chatService;

    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<Void> sendMessage(
            @PathVariable String conversationId,
            @RequestBody ChatRequest req) {

        chatService.saveMessage(
                conversationId,
                req.getSender(),
                req.getContent()
        );

        return ResponseEntity.ok().build();
    }
}
