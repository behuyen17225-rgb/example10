package com.nguyenthithuhuyen.example10.repository;

import com.nguyenthithuhuyen.example10.entity.ChatMessage;
import com.nguyenthithuhuyen.example10.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * Lấy lịch sử chat của user, sắp xếp theo mới nhất
     */
    List<ChatMessage> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Lấy lịch sử chat của user với phân trang
     */
    Page<ChatMessage> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /**
     * Lấy các tin nhắn gần đây nhất của user (dùng cho context conversation)
     * Implemented with Pageable to avoid native LIMIT parameter issues.
     */
    org.springframework.data.domain.Page<ChatMessage> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    default List<ChatMessage> findRecentMessages(Long userId, int limit) {
        return findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, Math.max(1, limit))).getContent();
    }

    /**
     * Xóa toàn bộ chat history của user
     */
    void deleteByUser(User user);
}
