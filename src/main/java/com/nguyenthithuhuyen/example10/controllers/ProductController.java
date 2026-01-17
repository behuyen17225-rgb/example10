package com.nguyenthithuhuyen.example10.controllers;

import com.nguyenthithuhuyen.example10.dto.ProductResponseDto;
import com.nguyenthithuhuyen.example10.entity.Category;
import com.nguyenthithuhuyen.example10.entity.Product;
import com.nguyenthithuhuyen.example10.repository.CategoryRepository;
import com.nguyenthithuhuyen.example10.security.services.ProductService;
import com.nguyenthithuhuyen.example10.mapper.ProductMapper;
import com.nguyenthithuhuyen.example10.dto.ProductRequest;
import com.nguyenthithuhuyen.example10.entity.ProductPrice;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final CategoryRepository categoryRepository;


    /* ================= GET ALL ================= */
    @GetMapping
    public ResponseEntity<List<ProductResponseDto>> getAllProducts() {
        return ResponseEntity.ok(
            productService.getAllProducts()
                .stream()
                .map(ProductMapper::toResponse)
                .toList()
        );
    }

    /* ================= GET BY ID ================= */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(
            ProductMapper.toResponse(productService.getProductById(id))
        );
    }

    /* ================= CREATE ================= */
    @PostMapping
    public ResponseEntity<ProductResponseDto> create(
            @RequestBody ProductRequest request) {

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .stockQuantity(request.getStockQuantity())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .category(category)
                .build();

        // 🔥 map prices theo size
        if (request.getPrices() != null) {
            List<ProductPrice> prices = request.getPrices().stream()
                .map(p -> ProductPrice.builder()
                        .size(p.getSize())   // String hoặc Enum
                        .price(p.getPrice())
                        .product(product)
                        .build()
                ).toList();

            product.setPrices(prices);
        }

        Product saved = productService.createProduct(product);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ProductMapper.toResponse(saved));
    }

    /* ================= UPDATE ================= */
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDto> update(
            @PathVariable Long id,
            @RequestBody ProductRequest request) {

        Product product = productService.getProductById(id);

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setImageUrl(request.getImageUrl());
        product.setStockQuantity(request.getStockQuantity());
        product.setIsActive(request.getIsActive());
        product.setCategory(category);

        // 🔥 replace prices
        product.getPrices().clear();
        if (request.getPrices() != null) {
            request.getPrices().forEach(p ->
                product.getPrices().add(
                    ProductPrice.builder()
                        .size(p.getSize())
                        .price(p.getPrice())
                        .product(product)
                        .build()
                )
            );
        }

        return ResponseEntity.ok(
            ProductMapper.toResponse(productService.updateProduct(id, product))
        );
    }

    /* ================= DELETE ================= */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
