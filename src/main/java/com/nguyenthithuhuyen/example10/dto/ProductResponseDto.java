package com.nguyenthithuhuyen.example10.dto;

import lombok.Data;


import java.math.BigDecimal;

@Data
public class ProductResponseDto {
    public Long id;
    public String name;
    public String description;
    public BigDecimal price;
    public String imageUrl;
    public Integer stockQuantity;
    public Boolean isActive;
    public CategoryDTO category; // <-- chỉ chứa thông tin cơ bản, tránh lặp
}
