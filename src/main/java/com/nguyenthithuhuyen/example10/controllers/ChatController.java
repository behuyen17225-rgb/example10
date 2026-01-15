package com.nguyenthithuhuyen.example10.controllers;

import com.nguyenthithuhuyen.example10.dto.ProductResponseDto;
import com.nguyenthithuhuyen.example10.mapper.ProductMapper;
import com.nguyenthithuhuyen.example10.payload.request.ChatRequest;
import com.nguyenthithuhuyen.example10.payload.response.ChatResponse;
import com.nguyenthithuhuyen.example10.repository.ProductRepository;
import com.nguyenthithuhuyen.example10.security.services.GeminiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin
public class ChatController {

    private final GeminiService geminiService;
    private final ProductRepository productRepo;

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest req) {

        // 1️⃣ Gọi Gemini AI
        String aiText = geminiService.askGemini(req.getPrompt());

        // 2️⃣ Tìm sản phẩm liên quan + map sang DTO
        List<ProductResponseDto> products = productRepo
                .findTop5ByNameContainingIgnoreCase(req.getPrompt())
                .stream()
                .map(ProductMapper::toResponse)
                .toList();

        // 3️⃣ Trả về cho FE
        return new ChatResponse(aiText, products);
    }
}
