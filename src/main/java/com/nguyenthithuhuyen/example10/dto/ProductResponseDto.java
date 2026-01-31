package com.nguyenthithuhuyen.example10.dto;

import lombok.Data;
import org.springframework.hateoas.RepresentationModel;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductResponseDto
        extends RepresentationModel<ProductResponseDto> {

    private Long id;
    private String name;
    private String description;

    // ❌ price đơn lẻ KHÔNG cần nếu bạn dùng prices + minPrice
    // private BigDecimal price;

    private String imageUrl;
    private Integer stockQuantity;
    private Boolean isActive;

    private BigDecimal minPrice;

    private CategoryDTO category;
    private List<ProductPriceDTO> prices;
}
