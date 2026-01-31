package com.nguyenthithuhuyen.example10.repository;

import com.nguyenthithuhuyen.example10.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;



import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findByStaffId(Long staffId);

    List<Conversation> findByCustomerId(Long customerId);
    List<Conversation> findByStaffIdOrderByUpdatedAtDesc(Long staffId);
 List<Conversation> findByCustomerIdOrderByUpdatedAtDesc(Long customerId);
}
