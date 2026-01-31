package com.nguyenthithuhuyen.example10.security.services;
import com.nguyenthithuhuyen.example10.dto.ConversationSidebarDto;
import com.nguyenthithuhuyen.example10.entity.Chat;
import com.nguyenthithuhuyen.example10.entity.Conversation;
import com.nguyenthithuhuyen.example10.repository.ChatRepository;
import com.nguyenthithuhuyen.example10.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;


@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepo;
    private final ChatRepository chatRepo;

    public List<ConversationSidebarDto> getSidebarForStaff(Long staffId) {

        List<Conversation> conversations =
                conversationRepo.findByStaffIdOrderByUpdatedAtDesc(staffId);

        return conversations.stream().map(conv -> {

            Chat lastMsg = chatRepo
                .findTopByConversationIdOrderByCreatedAtDesc(conv.getId())
                .orElse(null);

            return new ConversationSidebarDto(
                conv.getId(),
                conv.getCustomerId(),
                lastMsg != null ? lastMsg.getContent() : "",
                conv.getUpdatedAt()
            );
        }).toList();
    }
}
