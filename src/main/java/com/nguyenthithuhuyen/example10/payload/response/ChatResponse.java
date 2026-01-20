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
    private String messageType; // TEXT, PRODUCT

    public ChatResponse(String text, List<ProductResponseDto> products) {
        this.text = text;
        this.products = products;
        this.messageType = "TEXT";
    }

    public static ChatResponse products(String text, List<ProductResponseDto> products) {
        ChatResponse response = new ChatResponse(text, products);
        response.setMessageType("PRODUCT");
        return response;
    }

    public static ChatResponse text(String text) {
        return new ChatResponse(text, null);
    }
}
