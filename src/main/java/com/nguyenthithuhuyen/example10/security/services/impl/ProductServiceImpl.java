package com.nguyenthithuhuyen.example10.security.services.impl;

import com.nguyenthithuhuyen.example10.dto.ProductWithPromotionsDTO;
import com.nguyenthithuhuyen.example10.dto.PromotionDTO;
import com.nguyenthithuhuyen.example10.entity.Product;
import com.nguyenthithuhuyen.example10.entity.Promotion;
import com.nguyenthithuhuyen.example10.repository.ProductRepository;
import com.nguyenthithuhuyen.example10.security.services.ProductService;
import com.nguyenthithuhuyen.example10.security.services.PromotionProductService;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final PromotionProductService promotionProductService;

    /* ================= CREATE ================= */
    @Override
    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    /* ================= GET BY ID ================= */
    @Override
    public Product getProductById(Long productId) {
        return productRepository.findByIdWithCategory(productId)
                .orElseThrow(() ->
                        new RuntimeException("Product not found with id: " + productId));
    }

    /* ================= GET ALL ================= */
    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAllWithCategory();
    }

    /* ================= UPDATE ================= */
    @Override
    public Product updateProduct(Long productId, Product product) {

        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() ->
                        new RuntimeException("Product not found with id: " + productId));

        existingProduct.setName(product.getName());
        existingProduct.setDescription(product.getDescription());
        existingProduct.setImageUrl(product.getImageUrl());
        existingProduct.setCategory(product.getCategory());
        existingProduct.setStockQuantity(product.getStockQuantity());
        existingProduct.setIsActive(product.getIsActive());

        // ❌ KHÔNG set price
        // prices (size) update bằng API riêng

        return productRepository.save(existingProduct);
    }

    /* ================= DELETE ================= */
    @Override
    public void deleteProduct(Long productId) {
        productRepository.deleteById(productId);
    }

    /* ================= PRODUCTS WITH ACTIVE PROMOTIONS ================= */
    public List<ProductWithPromotionsDTO> getAllProductsWithActivePromotions() {

        List<Product> products = productRepository.findAllWithCategory();

        return products.stream().map(product -> {

            ProductWithPromotionsDTO dto = new ProductWithPromotionsDTO();
            dto.setId(product.getId());
            dto.setName(product.getName());
            dto.setImageUrl(product.getImageUrl());
            dto.setCategoryName(product.getCategory().getName());

            // ===== PROMOTIONS =====
            List<PromotionDTO> promotionDTOs =
                    promotionProductService.getByProductId(product.getId())
                            .stream()
                            .map(pp -> pp.getPromotion())
                            .filter(Promotion::getIsActive)
                            .map(promo -> {
                                PromotionDTO pdto = new PromotionDTO();
                                pdto.setName(promo.getName());
                                pdto.setDiscountPercent(promo.getDiscountPercent());
                                pdto.setDiscountAmount(promo.getDiscountAmount());
                                pdto.setIsActive(promo.getIsActive());
                                return pdto;
                            })
                            .collect(Collectors.toList());

            dto.setPromotions(promotionDTOs);
            return dto;

        }).collect(Collectors.toList());
    }
}
