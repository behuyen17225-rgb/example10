package com.nguyenthithuhuyen.example10.dto;

import lombok.Data;


import java.math.BigDecimal;
import java.util.List;
@Data
public class ProductResponseDto {
    public Long id;
    public String name;
    public String description;
    public BigDecimal price;
    public String imageUrl;
    public Integer stockQuantity;
    public Boolean isActive;
    public CategoryDTO category; 
    private List<ProductPriceDTO> prices;}
