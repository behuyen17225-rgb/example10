package com.nguyenthithuhuyen.example10.controllers;

import com.nguyenthithuhuyen.example10.entity.Address;
import com.nguyenthithuhuyen.example10.security.services.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    /* ===== CREATE ===== */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PostMapping
    public ResponseEntity<Address> createAddress(@RequestBody Address addressRequest) {
        String username = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        Address created = addressService.createAddress(username, addressRequest);
        return ResponseEntity.status(201).body(created);
    }

    /* ===== GET ALL ADDRESSES OF USER ===== */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/my")
    public ResponseEntity<List<Address>> getMyAddresses() {
        String username = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        List<Address> addresses = addressService.getAddressesByUsername(username);
        return ResponseEntity.ok(addresses);
    }

    /* ===== GET DEFAULT ADDRESS ===== */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/my/default")
    public ResponseEntity<?> getDefaultAddress() {
        String username = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        Address defaultAddress = addressService.getDefaultAddress(username);
        if (defaultAddress == null) {
            return ResponseEntity.status(404).body("No default address found");
        }
        return ResponseEntity.ok(defaultAddress);
    }

    /* ===== GET ONE ADDRESS ===== */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<Address> getAddressById(@PathVariable Long id) {
        String username = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        Address address = addressService.getAddressById(id);
        
        // Bảo mật: chỉ user chủ sở hữu mới có thể xem
        if (!address.getUser().getUsername().equals(username)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(address);
    }

    /* ===== UPDATE ADDRESS ===== */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Address> updateAddress(
            @PathVariable Long id,
            @RequestBody Address addressRequest) {

        String username = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        Address updated = addressService.updateAddress(id, username, addressRequest);
        return ResponseEntity.ok(updated);
    }

    /* ===== SET DEFAULT ADDRESS ===== */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PutMapping("/{id}/set-default")
    public ResponseEntity<Address> setDefaultAddress(@PathVariable Long id) {
        String username = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        Address updated = addressService.setDefaultAddress(id, username);
        return ResponseEntity.ok(updated);
    }

    /* ===== DELETE ADDRESS ===== */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAddress(@PathVariable Long id) {
        String username = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        try {
            addressService.deleteAddress(id, username);
            return ResponseEntity.ok().body("Address deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }
}
