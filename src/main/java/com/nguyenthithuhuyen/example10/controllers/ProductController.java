package com.nguyenthithuhuyen.example10.controllers;

import com.nguyenthithuhuyen.example10.dto.ProductRequest;
import com.nguyenthithuhuyen.example10.dto.ProductResponseDto;
import com.nguyenthithuhuyen.example10.entity.Category;
import com.nguyenthithuhuyen.example10.entity.Product;
import com.nguyenthithuhuyen.example10.entity.ProductPrice;
import com.nguyenthithuhuyen.example10.mapper.ProductMapper;
import com.nguyenthithuhuyen.example10.repository.CategoryRepository;
import com.nguyenthithuhuyen.example10.security.services.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final CategoryRepository categoryRepository;

    /* ================= GET ALL ================= */
    @GetMapping
    public ResponseEntity<List<ProductResponseDto>> getAllProducts() {

        List<ProductResponseDto> result = productService.getAllProducts()
                .stream()
                .map(p -> {
                    ProductResponseDto dto = ProductMapper.toResponse(p);

                    dto.add(linkTo(
                            methodOn(ProductController.class)
                                    .getById(p.getId()))
                            .withSelfRel());

                    return dto;
                })
                .toList();

        return ResponseEntity.ok(result);
    }

    /* ================= GET BY ID ================= */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDto> getById(@PathVariable Long id) {

        ProductResponseDto dto =
                ProductMapper.toResponse(productService.getProductById(id));

        dto.add(linkTo(
                methodOn(ProductController.class).getById(id))
                .withSelfRel());

        dto.add(linkTo(
                methodOn(ProductController.class).getAllProducts())
                .withRel("products"));

        return ResponseEntity.ok(dto);
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

        if (request.getPrices() != null) {
            List<ProductPrice> prices = request.getPrices().stream()
                    .map(p -> ProductPrice.builder()
                            .size(p.getSize())
                            .price(p.getPrice())
                            .product(product)
                            .build())
                    .toList();
            product.setPrices(prices);
        }

        Product saved = productService.createProduct(product);

        ProductResponseDto dto = ProductMapper.toResponse(saved);
        dto.add(linkTo(
                methodOn(ProductController.class)
                        .getById(saved.getId()))
                .withSelfRel());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(dto);
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

        Product updated = productService.updateProduct(id, product);

        ProductResponseDto dto = ProductMapper.toResponse(updated);
        dto.add(linkTo(
                methodOn(ProductController.class)
                        .getById(updated.getId()))
                .withSelfRel());

        return ResponseEntity.ok(dto);
    }

    /* ================= DELETE ================= */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
