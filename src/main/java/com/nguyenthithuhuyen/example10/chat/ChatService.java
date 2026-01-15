package com.nguyenthithuhuyen.example10.chat;

import com.nguyenthithuhuyen.example10.dto.ProductResponseDto;
import com.nguyenthithuhuyen.example10.mapper.ProductMapper;
import com.nguyenthithuhuyen.example10.payload.response.ChatResponse;
import com.nguyenthithuhuyen.example10.repository.ProductRepository;
import com.nguyenthithuhuyen.example10.security.services.GeminiService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ProductRepository productRepo;
    private final GeminiService geminiService;

    public ChatResponse handleChat(String prompt) {

        ChatIntent intent = parseIntent(prompt);

        List<ProductResponseDto> products = productRepo
                .searchByChat(
                        intent.getKeyword(),
                        intent.getMaxPrice(),
                        PageRequest.of(0, 5))
                .stream()
                .map(ProductMapper::toResponse)
                .toList();

        String aiText = buildReply(intent, products);

        return new ChatResponse(aiText, products);
    }

    /* ================= PARSE PROMPT ================= */

    private ChatIntent parseIntent(String prompt) {
        ChatIntent intent = new ChatIntent();

        String text = prompt.toLowerCase();

        // 🎂 dịp
        if (text.contains("sinh nhật"))
            intent.setOccasion("sinh nhật");

        // 👥 số người
        if (text.contains("2 người"))
            intent.setPeople(2);
        if (text.contains("4 người"))
            intent.setPeople(4);
        if (intent.getOccasion() == null) {
            intent.setOccasion("bữa tiệc");
        }

        if (intent.getPeople() == null) {
            intent.setPeople(4);
        }

        // 💰 giá
        intent.setMaxPrice(extractPrice(text));

        // 🍰 keyword
        if (text.contains("chocolate") || text.contains("socola"))
            intent.setKeyword("chocolate");
        else if (text.contains("trà xanh") || text.contains("matcha"))
            intent.setKeyword("trà xanh");
        else
            intent.setKeyword("bánh kem");

        return intent;
    }

    private Integer extractPrice(String text) {
        try {
            if (text.contains("k")) {
                int num = Integer.parseInt(text.replaceAll("\\D+", ""));
                return num * 1000;
            }
            if (text.contains("tr") || text.contains("triệu")) {
                int num = Integer.parseInt(text.replaceAll("\\D+", ""));
                return num * 1_000_000;
            }
        } catch (Exception ignored) {
        }

        return 500_000; // mặc định
    }
    /* ================= REPLY ================= */

    private String buildReply(ChatIntent intent, List<ProductResponseDto> products) {

        if (products.isEmpty()) {
            return "Dạ hiện quán chưa có bánh phù hợp mức giá này 😥 "
                    + "Bạn tăng ngân sách giúp em nha 💕";
        }

        return "Dạ em gợi ý vài mẫu bánh "
                + intent.getKeyword()
                + " phù hợp cho "
                + intent.getOccasion()
                + " nè 🍰\n"
                + "Bánh có thể ghi chữ + chọn size luôn ạ 💖";
    }
}
