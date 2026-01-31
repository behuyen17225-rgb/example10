package com.nguyenthithuhuyen.example10.controllers;

import com.nguyenthithuhuyen.example10.entity.Chat;
import com.nguyenthithuhuyen.example10.entity.Conversation;
import com.nguyenthithuhuyen.example10.repository.ChatRepository;
import com.nguyenthithuhuyen.example10.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
@CrossOrigin("*")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationRepository conversationRepo;
    private final ChatRepository chatRepo;

    /* ================== CREATE ================== */
    @PostMapping
    public Conversation createConversation(
            @RequestParam Long customerId,
            @RequestParam(required = false) Long staffId
    ) {
        Conversation c = new Conversation();
        c.setId(UUID.randomUUID().toString());
        c.setCustomerId(customerId);
        c.setStaffId(staffId);
        return conversationRepo.save(c);
    }

    /* ================== STAFF ================== */
    @GetMapping("/staff/{staffId}")
    public List<Conversation> getForStaff(@PathVariable Long staffId) {
        return conversationRepo.findByStaffIdOrderByUpdatedAtDesc(staffId);
    }

    /* ================== CUSTOMER ================== */
    @GetMapping("/customer/{customerId}")
    public List<Conversation> getForCustomer(@PathVariable Long customerId) {
        return conversationRepo.findByCustomerIdOrderByUpdatedAtDesc(customerId);
    }

    /* ================== CHAT HISTORY ================== */
    @GetMapping("/{conversationId}/messages")
    public List<Chat> getMessages(@PathVariable String conversationId) {
        return chatRepo.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }
}
