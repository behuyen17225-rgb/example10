package com.nguyenthithuhuyen.example10.entity;
import jakarta.persistence.*;
import lombok.Data; 
import java.time.LocalDateTime;

@Entity
@Table(name = "chat")
@Data
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;   // ✅ Long

    @Column(nullable = false)
    private String sender;

    @Column(nullable = false)
    private String content;

    private LocalDateTime createdAt = LocalDateTime.now();
}
