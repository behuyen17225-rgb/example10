package com.nguyenthithuhuyen.example10.dto;
import lombok.Data;
import java.util.List;


@Data
public class ProductRequest {
    private String name;
    private String description;
    private Long categoryId;
    private Integer stockQuantity;
    private Boolean isActive;
    private String imageUrl;

    // 🔥 prices theo size
    private List<ProductPriceDTO> prices;
}
