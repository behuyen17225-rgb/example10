package com.nguyenthithuhuyen.example10.entity;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;


@Entity
@Table(name = "conversations")
@Data
public class Conversation {

    @Id
    private String id;

    private Long customerId;
    private Long staffId;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
