package com.nguyenthithuhuyen.example10.controllers;

import com.nguyenthithuhuyen.example10.entity.Bill;
import com.nguyenthithuhuyen.example10.payload.request.BillRequest;
import com.nguyenthithuhuyen.example10.security.services.BillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
public class BillController {

    private final BillService billService;

    // ✅ Tạo hóa đơn: Cho phép Nhân viên và Admin
    @PostMapping
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')") // SỬA ĐỔI QUYỀN TRUY CẬP
    public ResponseEntity<?> createBill(@RequestBody BillRequest request) {
        try {
            Bill createdBill = billService.create(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdBill);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Lỗi tạo hóa đơn: " + e.getMessage());
        }
    }

    // ✅ Lấy danh sách hóa đơn: Chỉ Staff/Admin được xem
    @GetMapping
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')") // BỔ SUNG QUYỀN TRUY CẬP
    public ResponseEntity<?> getAllBills() {
        return ResponseEntity.ok(billService.getAll());
    }

    // ✅ Lấy hóa đơn theo ID: Chỉ Staff/Admin được xem
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')") // BỔ SUNG QUYỀN TRUY CẬP
    public ResponseEntity<?> getBillById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(billService.getById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // ✅ Cập nhật hóa đơn: Cho phép Nhân viên và Admin
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')") // SỬA ĐỔI QUYỀN TRUY CẬP
    public ResponseEntity<?> updateBill(@PathVariable Long id, @RequestBody Bill bill) {
        try {
            return ResponseEntity.ok(billService.update(id, bill));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Lỗi cập nhật hóa đơn: " + e.getMessage());
        }
    }

    // ✅ Xóa hóa đơn: Chỉ Admin được xóa
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')") // BỔ SUNG QUYỀN TRUY CẬP
    public ResponseEntity<?> deleteBill(@PathVariable Long id) {
        billService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ✅ Xuất hóa đơn ra PDF: Cho phép Staff/Admin
}