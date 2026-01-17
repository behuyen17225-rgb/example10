package com.nguyenthithuhuyen.example10.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ================= ORDER ================= */
    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonBackReference
    private Order order;

    /* ================= PRODUCT ================= */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /* ================= SIZE (BÁNH S / M / L) ================= */
    @Column(nullable = false, length = 10)
    private String size;

    /* ================= QUANTITY ================= */
    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    /* ================= UNIT PRICE ================= */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /* ================= SUBTOTAL ================= */
    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    /* ================= AUDIT ================= */
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        normalize();
        subtotal = price.multiply(BigDecimal.valueOf(quantity));
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        normalize();
        subtotal = price.multiply(BigDecimal.valueOf(quantity));
        updatedAt = LocalDateTime.now();
    }

    private void normalize() {
        if (quantity == null || quantity <= 0) quantity = 1;
        if (price == null) price = BigDecimal.ZERO;
    }
}
