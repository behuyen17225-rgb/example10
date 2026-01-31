package com.nguyenthithuhuyen.example10.mapper;

import java.math.BigDecimal;
import java.util.List;

import com.nguyenthithuhuyen.example10.dto.CategoryDTO;
import com.nguyenthithuhuyen.example10.dto.ProductPriceDTO;
import com.nguyenthithuhuyen.example10.dto.ProductResponseDto;
import com.nguyenthithuhuyen.example10.entity.Product;
import com.nguyenthithuhuyen.example10.entity.ProductPrice;

public class ProductMapper {

    public static ProductResponseDto toResponse(Product product) {

        ProductResponseDto dto = new ProductResponseDto();

        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setImageUrl(product.getImageUrl());
        dto.setStockQuantity(product.getStockQuantity());
        dto.setIsActive(product.getIsActive());

        /* ================= CATEGORY ================= */
        if (product.getCategory() != null) {
            dto.setCategory(
                new CategoryDTO(
                    product.getCategory().getId(),
                    product.getCategory().getName(),
                    product.getCategory().getSlug(),
                    product.getCategory().getDescription(),
                    product.getCategory().getParent() != null
                        ? product.getCategory().getParent().getId()
                        : null,
                    product.getCategory().getParent() != null
                        ? product.getCategory().getParent().getName()
                        : null
                )
            );
        }

        /* ================= PRICES ================= */
        List<ProductPrice> prices =
                product.getPrices() != null ? product.getPrices() : List.of();

        // map price DTO
        dto.setPrices(
            prices.stream()
                .map(p -> new ProductPriceDTO(
                    p.getSize(),
                    p.getPrice()
                ))
                .toList()
        );

        // min price
        BigDecimal minPrice = prices.stream()
                .map(ProductPrice::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        dto.setMinPrice(minPrice);

        return dto;
    }
}
