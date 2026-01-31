package com.nguyenthithuhuyen.example10.repository;
import com.nguyenthithuhuyen.example10.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository
        extends JpaRepository<Chat, Long> {

    List<Chat> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
    Optional<Chat> findTopByConversationIdOrderByCreatedAtDesc(Long conversationId);
}
