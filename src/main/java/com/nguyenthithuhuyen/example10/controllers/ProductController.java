package com.nguyenthithuhuyen.example10.controllers;

import com.nguyenthithuhuyen.example10.dto.CategoryDTO;
import com.nguyenthithuhuyen.example10.dto.ProductResponseDto;
import com.nguyenthithuhuyen.example10.dto.ProductWithPromotionsDTO;
import com.nguyenthithuhuyen.example10.entity.Category;
import com.nguyenthithuhuyen.example10.entity.Product;
import com.nguyenthithuhuyen.example10.repository.CategoryRepository;
import com.nguyenthithuhuyen.example10.security.services.ProductService;
import com.nguyenthithuhuyen.example10.security.services.impl.ProductServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final CategoryRepository categoryRepository;
    private final ProductServiceImpl productWithPromotionService;

    // ================= GET ALL PRODUCTS =================
    @GetMapping
    public ResponseEntity<List<ProductResponseDto>> getAllProducts() {
        List<ProductResponseDto> list = productService.getAllProducts()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    // ================= GET PRODUCT BY ID =================
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDto> getProductById(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        return ResponseEntity.ok(mapToDto(product));
    }

    // ================= CREATE PRODUCT =================
    // @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ProductResponseDto> createProduct(@RequestBody ProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .imageUrl(request.getImageUrl())
                .category(category)
                .build();

        Product saved = productService.createProduct(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDto(saved));
    }

    // ================= UPDATE PRODUCT =================
    // @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDto> updateProduct(
            @PathVariable Long id,
            @RequestBody ProductRequest request) {

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .isActive(request.getIsActive())
                .imageUrl(request.getImageUrl())
                .category(category)
                .build();

        Product updated = productService.updateProduct(id, product);
        return ResponseEntity.ok(mapToDto(updated));
    }

    // ================= DELETE PRODUCT =================
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok("Product deleted");
    }

    // ================= PRODUCTS WITH ACTIVE PROMOTIONS =================
    @GetMapping("/with-active-promotions")
    public ResponseEntity<List<ProductWithPromotionsDTO>> getProductsWithPromotions() {
        return ResponseEntity.ok(
                productWithPromotionService.getAllProductsWithActivePromotions()
        );
    }

    // ================= DTO MAPPER =================
    private ProductResponseDto mapToDto(Product p) {
        ProductResponseDto dto = new ProductResponseDto();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setDescription(p.getDescription());
        dto.setPrice(p.getPrice());
        dto.setImageUrl(p.getImageUrl());
        dto.setStockQuantity(p.getStockQuantity());
        dto.setIsActive(p.getIsActive());

        if (p.getCategory() != null) {
            Category c = p.getCategory();
            dto.setCategory(new CategoryDTO(
                    c.getId(),
                    c.getName(),
                    c.getSlug(),
                    c.getDescription(),
                    c.getParent() != null ? c.getParent().getId() : null,
                    c.getParent() != null ? c.getParent().getName() : null
            ));
        }

        return dto;
    }

    // ================= REQUEST DTO =================
    public static class ProductRequest {
        private String name;
        private String description;
        private BigDecimal price;
        private Long categoryId;
        private Integer stockQuantity;
        private Boolean isActive;
        private String imageUrl;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }

        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

        public Integer getStockQuantity() { return stockQuantity; }
        public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }

        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }

        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    }
}
