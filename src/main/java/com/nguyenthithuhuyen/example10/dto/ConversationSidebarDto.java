package com.nguyenthithuhuyen.example10.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ConversationSidebarDto {
    private String conversationId;
    private Long customerId;
    private String lastMessage;
    private LocalDateTime updatedAt;
}
