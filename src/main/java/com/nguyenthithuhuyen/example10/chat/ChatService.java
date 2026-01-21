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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ProductRepository productRepo;
    private final ChatMessageRepository chatMessageRepo;
    private final UserRepository userRepo;
    private final GeminiService geminiService; // CHỈ dùng cho chat thường

    // ===== HANDLE CHAT =====
    public ChatResponse handleChat(String message, Long userId) {

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ChatResponse response;

        try {
            String lowerMsg = message.toLowerCase();

            // 1️⃣ PHÂN TÍCH Ý ĐỊNH (RULE-BASED)
            String keyword = extractKeyword(lowerMsg);
            PriceRange priceRange = extractPriceRange(lowerMsg);

            boolean isProductQuestion =
                    keyword != null ||
                    lowerMsg.contains("bánh") ||
                    lowerMsg.contains("sản phẩm") ||
                    lowerMsg.contains("giá") ||
                    lowerMsg.contains("dưới") ||
                    lowerMsg.contains("trên");

            // 2️⃣ NẾU HỎI SẢN PHẨM → DB
            if (isProductQuestion) {

                List<ProductResponseDto> products = productRepo
                        .filterProducts(
                                keyword,
                                priceRange != null ? priceRange.minPrice : null,
                                priceRange != null ? priceRange.maxPrice : null
                        )
                        .stream()
                        .map(ProductMapper::toResponse)
                        .toList();

                if (products.isEmpty()) {
                    response = ChatResponse.text(
                            "😥 Hiện chưa có " +
                            (keyword != null ? keyword : "sản phẩm") +
                            " " + buildPriceRangeText(priceRange)
                    );
                } else {
                    response = ChatResponse.products(
                            buildSuggestionText(keyword, priceRange),
                            products
                    );
                }
            }
            // 3️⃣ CHAT THƯỜNG → GEMINI (KHÔNG RETRY)
            else {
                String aiText;
                try {
                    aiText = geminiService.askGeminiGeneral(message, "");
                    if (aiText == null || aiText.isBlank()) {
                        aiText = "Em có thể giúp bạn tìm bánh hoặc xem giá nha 😊";
                    }
                } catch (Exception e) {
                    aiText = "Em đang hơi bận, bạn thử lại sau giúp em nha 😊";
                }
                response = ChatResponse.text(aiText);
            }

        } catch (Exception e) {
            e.printStackTrace();
            response = ChatResponse.text("⚠️ Có lỗi xảy ra, bạn thử lại sau giúp em nha!");
        }

        // 4️⃣ LƯU CHAT
        chatMessageRepo.save(
                ChatMessage.builder()
                        .user(user)
                        .userMessage(message)
                        .aiResponse(response.getText())
                        .messageType(response.getMessageType())
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        return response;
    }

    // ===== HELPER =====

    private static class PriceRange {
        BigDecimal minPrice;
        BigDecimal maxPrice;
        PriceRange(BigDecimal min, BigDecimal max) {
            this.minPrice = min;
            this.maxPrice = max;
        }
    }

    // ===== KEYWORD =====
    private String extractKeyword(String msg) {
        if (msg.contains("bánh kem")) return "bánh kem";
        if (msg.contains("bánh su")) return "bánh su";
        if (msg.contains("bánh")) return "bánh";
        if (msg.contains("socola")) return "socola";
        if (msg.contains("matcha")) return "matcha";
        if (msg.contains("vanilla")) return "vanilla";
        return null;
    }

    // ===== PRICE RANGE =====
    private PriceRange extractPriceRange(String msg) {

        if (msg.matches(".*từ\\s*\\d+\\s*k\\s*đến\\s*\\d+\\s*k.*")) {
            String[] nums = msg.replaceAll("[^0-9 ]", "").trim().split("\\s+");
            return new PriceRange(
                    new BigDecimal(nums[0]).multiply(BigDecimal.valueOf(1000)),
                    new BigDecimal(nums[1]).multiply(BigDecimal.valueOf(1000))
            );
        }

        if (msg.contains("dưới")) {
            BigDecimal p = extractPrice(msg);
            return p != null ? new PriceRange(null, p) : null;
        }

        if (msg.contains("trên")) {
            BigDecimal p = extractPrice(msg);
            return p != null ? new PriceRange(p, null) : null;
        }

        return null;
    }

    private BigDecimal extractPrice(String msg) {
        String num = msg.replaceAll("[^0-9]", "");
        if (num.isEmpty()) return null;
        return new BigDecimal(num).multiply(BigDecimal.valueOf(1000));
    }

    // ===== TEXT BUILDER =====
    private String buildPriceRangeText(PriceRange pr) {
        if (pr == null) return "";
        if (pr.minPrice != null && pr.maxPrice != null)
            return "từ " + pr.minPrice.longValue()/1000 + "k đến " + pr.maxPrice.longValue()/1000 + "k";
        if (pr.maxPrice != null)
            return "dưới " + pr.maxPrice.longValue()/1000 + "k";
        if (pr.minPrice != null)
            return "trên " + pr.minPrice.longValue()/1000 + "k";
        return "";
    }

    private String buildSuggestionText(String keyword, PriceRange pr) {
        String text = "🧁 Em gợi ý ";
        text += (keyword != null ? keyword : "sản phẩm");
        if (pr != null) text += " " + buildPriceRangeText(pr);
        return text + " cho bạn nè";
    }
}
