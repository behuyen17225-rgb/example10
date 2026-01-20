package com.nguyenthithuhuyen.example10.payload.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private String prompt;
    private Long userId; // ID của user đang chat
}
