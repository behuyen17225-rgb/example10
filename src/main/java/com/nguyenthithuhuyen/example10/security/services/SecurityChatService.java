package com.nguyenthithuhuyen.example10.security.services;
import com.nguyenthithuhuyen.example10.entity.Chat;
import com.nguyenthithuhuyen.example10.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class SecurityChatService {

    private final ChatRepository chatRepo;

    public void saveMessage(Long conversationId, String sender, String content) {

        Chat chat = new Chat();
        chat.setConversationId(conversationId); // ✅ Long
        chat.setSender(sender);
        chat.setContent(content);

        chatRepo.save(chat);
    }
}
