package com.nguyenthithuhuyen.example10.controllers;

import com.nguyenthithuhuyen.example10.entity.Chat;
import com.nguyenthithuhuyen.example10.repository.ChatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@CrossOrigin("*")
public class ConversationController {

    @Autowired
    ChatRepository repo;

    @GetMapping("/{conversationId}/messages")
    public List<Chat> history(@PathVariable String conversationId) {
        return repo.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }
}
