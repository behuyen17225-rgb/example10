package com.nguyenthithuhuyen.example10.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private Long id;
    private String userMessage;
    private String aiResponse;
    private String messageType;
    private LocalDateTime createdAt;
}
