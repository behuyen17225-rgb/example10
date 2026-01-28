package com.nguyenthithuhuyen.example10.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.nguyenthithuhuyen.example10.entity.enums.OrderStatus;
import com.nguyenthithuhuyen.example10.entity.enums.OrderType;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ================= USER ================= */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private User user;

    /* ================= TYPE ================= */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType orderType;

    /* ================= TABLE ================= */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id")
    private TableEntity table;

    /* ================= STATUS ================= */
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    /* ================= MONEY ================= */
    private BigDecimal totalAmount;
    private BigDecimal discount;
    private BigDecimal finalAmount;

    /* ================= CUSTOMER ================= */
    private String customerName;
    private String phone;
    private String address;

    /* ================= PAYMENT ================= */
    @Column(nullable = false)
private String paymentMethod;

    private String paymentRef;
    private LocalDateTime paidAt;

    /* ================= PREORDER ================= */
    private LocalDateTime pickupTime;

    /* ================= ITEMS ================= */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
