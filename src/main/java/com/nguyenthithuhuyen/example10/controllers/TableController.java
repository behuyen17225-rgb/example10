package com.nguyenthithuhuyen.example10.controllers;

import com.nguyenthithuhuyen.example10.entity.TableEntity;
import com.nguyenthithuhuyen.example10.entity.enums.Status;
import com.nguyenthithuhuyen.example10.security.services.QrCodeService;
import com.nguyenthithuhuyen.example10.security.services.TableService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/tables")
@RequiredArgsConstructor
public class TableController {

    private final TableService tableService;
    private final QrCodeService qrCodeService;

    // -------------------------------------------------------------------------
    // 1. CREATE TABLE (ADMIN)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<TableEntity> create(@RequestBody TableEntity table) {

        if (table.getTableNumber() == null) {
            return ResponseEntity.badRequest().build();
        }

        table.setStatus(Status.FREE);
        table.setCreatedAt(LocalDateTime.now());
        table.setUpdatedAt(LocalDateTime.now());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(tableService.createTable(table));
    }

    // -------------------------------------------------------------------------
    // 2. READ TABLE

    // Lấy tất cả bàn
    @GetMapping
    public ResponseEntity<List<TableEntity>> getAll() {
        return ResponseEntity.ok(tableService.getAllTables());
    }

    // Lấy bàn theo ID
    @GetMapping("/{id}")
    public ResponseEntity<TableEntity> getById(@PathVariable Long id) {
        return ResponseEntity.ok(tableService.getTableById(id));
    }

    // Lấy bàn đang trống
    @GetMapping("/available")
    public ResponseEntity<List<TableEntity>> getAvailableTables() {
        return ResponseEntity.ok(tableService.getTablesByStatus(Status.FREE));
    }

    // Lấy bàn theo code (QR)
    @GetMapping("/code/{code}")
    public ResponseEntity<TableEntity> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(tableService.getTableByCode(code));
    }

    // -------------------------------------------------------------------------
    // 3. UPDATE TABLE (ADMIN / MODERATOR)
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    @PutMapping("/{id}")
    public ResponseEntity<TableEntity> update(
            @PathVariable Long id,
            @RequestBody TableEntity updateData) {

        TableEntity table = tableService.getTableById(id);

        if (updateData.getTableNumber() != null) {
            table.setTableNumber(updateData.getTableNumber());
        }

        if (updateData.getCapacity() != null) {
            table.setCapacity(updateData.getCapacity());
        }

        if (updateData.getStatus() != null) {
            table.setStatus(updateData.getStatus());
        }

        table.setUpdatedAt(LocalDateTime.now());

        return ResponseEntity.ok(tableService.updateTable(id, table));
    }

    // -------------------------------------------------------------------------
    // 4. DELETE TABLE (ADMIN)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        TableEntity table = tableService.getTableById(id);

        if (table.getStatus() != Status.FREE) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        tableService.deleteTable(id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // 5. GET QR CODE
    @GetMapping("/{id}/qr")
    public ResponseEntity<byte[]> getTableQr(@PathVariable Long id) {

        TableEntity table = tableService.getTableById(id);
        byte[] qrImage = qrCodeService.generateQrCode(table.getCode());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=table_" + table.getCode() + ".png")
                .contentType(MediaType.IMAGE_PNG)
                .body(qrImage);
    }
    @PutMapping("/{id}/reserve")
public ResponseEntity<?> reserve(@PathVariable Long id) {
    TableEntity table = tableService.getTableById(id);

    if (table.getStatus() != Status.FREE) {
        return ResponseEntity.badRequest().body("Table not free");
    }

    table.setStatus(Status.OCCUPIED);
    tableService.updateTable(id, table);

    return ResponseEntity.ok().build();
}

}
