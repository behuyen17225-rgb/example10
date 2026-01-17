package com.nguyenthithuhuyen.example10.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ================= USER ================= */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    /* ================= ADDRESS INFO ================= */
    @Column(name = "street_address", nullable = false)
    private String streetAddress;

    @Column(name = "ward", nullable = false)
    private String ward;

    @Column(name = "district", nullable = false)
    private String district;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "recipient_name", nullable = false)
    private String recipientName;

    /* ================= DEFAULT FLAG ================= */
    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    /* ================= TIMESTAMPS ================= */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
