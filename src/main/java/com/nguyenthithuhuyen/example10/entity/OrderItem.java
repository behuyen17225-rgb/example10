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

    /* ================= SIZE (QUAN TRỌNG) ================= */
    @Column(nullable = false, length = 10)
    private String size;   // 👈 THÊM FIELD NÀY

    /* ================= QUANTITY ================= */
    @Column(nullable = false)
    private Integer quantity = 1;

    /* ================= PRICE ================= */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    /* ================= AUDIT ================= */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /* ================= LIFECYCLE ================= */
    @PrePersist
    protected void onCreateItem() {
        normalize();
        calculateSubtotal();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdateItem() {
        normalize();
        calculateSubtotal();
        updatedAt = LocalDateTime.now();
    }

    /* ================= HELPER ================= */
    private void normalize() {
        if (quantity == null || quantity <= 0) {
            quantity = 1;
        }
        if (price == null) {
            price = BigDecimal.ZERO;
        }
    }

    private void calculateSubtotal() {
        subtotal = price.multiply(BigDecimal.valueOf(quantity));
    }
}
