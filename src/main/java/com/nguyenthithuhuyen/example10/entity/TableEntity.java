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

    // 👉 GIỮ field number, map sang cột khác + xử lý null
    @Column(name = "table_no", nullable = false)
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

        // ✅ xử lý number = null
        if (this.number == null) {
            this.number = 0;
        }

        if (this.code == null || this.code.isBlank()) {
            this.code = "T" + System.currentTimeMillis();
        }

        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
