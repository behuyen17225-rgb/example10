package com.nguyenthithuhuyen.example10.entity;

import com.nguyenthithuhuyen.example10.entity.enums.Status;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "restaurant_tables")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Integer tableNumber;

    @Column(nullable = false)
    @Builder.Default
    private Integer capacity = 4;

    // 👉 cột mới dùng để ghi
    @Column(name = "table_no", nullable = false)
    private Integer tableNo;

    // 👉 cột cũ legacy
    @Column(name = "number", insertable = false, updatable = false)
    private Integer number;

    @Column(unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.FREE;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {

        if (tableNo == null) {
            tableNo = tableNumber; // hoặc 0 nếu bạn muốn
        }

        if (code == null || code.isBlank()) {
            code = "T" + System.currentTimeMillis();
        }

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
