package com.nguyenthithuhuyen.example10.payload.response;

import com.nguyenthithuhuyen.example10.dto.ProductResponseDto;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ChatResponse {
    private String text;
    private List<ProductResponseDto> products;
}
