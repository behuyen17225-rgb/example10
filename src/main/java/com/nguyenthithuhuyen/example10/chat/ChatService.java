package com.nguyenthithuhuyen.example10.chat;

import com.nguyenthithuhuyen.example10.dto.ProductResponseDto;
import com.nguyenthithuhuyen.example10.entity.ChatMessage;
import com.nguyenthithuhuyen.example10.entity.User;
import com.nguyenthithuhuyen.example10.mapper.ProductMapper;
import com.nguyenthithuhuyen.example10.payload.response.ChatResponse;
import com.nguyenthithuhuyen.example10.repository.ChatMessageRepository;
import com.nguyenthithuhuyen.example10.repository.ProductRepository;
import com.nguyenthithuhuyen.example10.repository.UserRepository;
import com.nguyenthithuhuyen.example10.security.services.GeminiService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final GeminiService geminiService;
    private final ProductRepository productRepo;
    private final ChatMessageRepository chatMessageRepo;
    private final UserRepository userRepo;

    /**
     * Xử lý tin nhắn chat từ user và lưu lịch sử
     */
    public ChatResponse handleChat(String message, Long userId) {

        // Lấy user từ DB
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Lấy lịch sử chat gần đây (5 lần gần nhất) để cung cấp context
        List<ChatMessage> conversationHistory = chatMessageRepo.findRecentMessages(userId, 5);
        
        // Gọi AI để xử lý intent
        Map<String, Object> ai = geminiService.askGeminiForIntent(message);
        String intent = ai.getOrDefault("intent", "UNKNOWN").toString();

        String keyword = (String) ai.get("keyword");
        BigDecimal maxPrice = null;

        if (ai.get("maxPrice") != null) {
            maxPrice = new BigDecimal(ai.get("maxPrice").toString());
        }

        ChatResponse response = null;

        /* ===== SHOW / FILTER PRODUCTS ===== */
        if (intent.equals("SHOW_PRODUCTS") || intent.equals("FILTER_PRICE")) {

            List<ProductResponseDto> products =
                productRepo.searchByChat(
                        keyword,
                        maxPrice,
                        PageRequest.of(0, 5)
                )
                .stream()
                .map(ProductMapper::toResponse)
                .toList();

            if (products.isEmpty()) {
                response = ChatResponse.text(
                    "Dạ hiện chưa có bánh phù hợp mức giá này 😥"
                );
                response.setMessageType("TEXT");
            } else {
                response = ChatResponse.products(
                    "Em gợi ý vài mẫu bánh phù hợp cho bạn nè",
                    products
                );
                response.setMessageType("PRODUCT");
            }
        }
        /* ===== TRACK ORDER ===== */
        else if (intent.equals("TRACK_ORDER")) {
            response = ChatResponse.text(
                "Bạn gửi giúp em mã đơn hàng để em kiểm tra nha 📦"
            );
            response.setMessageType("TEXT");
        }
        /* ===== GENERAL AI CHAT ===== */
        else {
            // Gọi AI để trả lời câu hỏi chung
            String aiAnswer = geminiService.askGeminiGeneral(message, convertToString(conversationHistory));
            response = ChatResponse.text(aiAnswer);
            response.setMessageType("TEXT");
        }

        // Lưu chat message vào DB
        ChatMessage chatMsg = ChatMessage.builder()
            .user(user)
            .userMessage(message)
            .aiResponse(response.getText())
            .messageType(response.getMessageType())
            .createdAt(LocalDateTime.now())
            .build();
        chatMessageRepo.save(chatMsg);

        return response;
    }

    /**
     * Lấy lịch sử chat của user
     */
    public List<ChatMessage> getChatHistory(Long userId) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        return chatMessageRepo.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Xóa toàn bộ chat history của user
     */
    public void clearChatHistory(Long userId) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        chatMessageRepo.deleteByUser(user);
    }

    /**
     * Chuyển đổi lịch sử chat thành string để gửi cho AI
     */
    private String convertToString(List<ChatMessage> messages) {
        return messages.stream()
            .map(msg -> String.format("User: %s\nAI: %s", msg.getUserMessage(), msg.getAiResponse()))
            .collect(Collectors.joining("\n\n"));
    }
}
