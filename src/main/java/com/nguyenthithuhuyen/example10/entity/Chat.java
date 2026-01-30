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

    @Column(nullable = false)
    private String conversationId;

    @Column(nullable = false)
    private String sender; // CUSTOMER | STAFF

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
