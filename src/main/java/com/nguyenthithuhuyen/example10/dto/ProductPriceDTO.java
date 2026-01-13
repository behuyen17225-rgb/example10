package com.nguyenthithuhuyen.example10.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductPriceDTO {
    private String size;        // S, M, L
    private BigDecimal price;   // giá theo size
}
