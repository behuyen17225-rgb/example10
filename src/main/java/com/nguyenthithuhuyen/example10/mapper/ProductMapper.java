package com.nguyenthithuhuyen.example10.mapper;

import java.util.List;
import java.util.stream.Collectors;
import com.nguyenthithuhuyen.example10.dto.CategoryDTO;
import com.nguyenthithuhuyen.example10.dto.ProductPriceDTO;
import com.nguyenthithuhuyen.example10.dto.ProductResponseDto;
import com.nguyenthithuhuyen.example10.entity.Product;


public class ProductMapper {

    public static ProductResponseDto toResponse(Product product) {

        ProductResponseDto dto = new ProductResponseDto();

        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setImageUrl(product.getImageUrl());
        dto.setStockQuantity(product.getStockQuantity());
        dto.setIsActive(product.getIsActive());

        // ✅ CATEGORY
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

        // ✅ PRICES (SIZE)
        dto.setPrices(
            product.getPrices() == null
                ? List.of()
                : product.getPrices().stream()
                    .map(p -> new ProductPriceDTO(
                        p.getSize(),

                        p.getPrice()
                    ))
                    .toList()
        );

        return dto;
    }
}
