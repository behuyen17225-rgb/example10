package com.nguyenthithuhuyen.example10.security.services.impl;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import com.nguyenthithuhuyen.example10.entity.Category;
import com.nguyenthithuhuyen.example10.repository.CategoryRepository;
import com.nguyenthithuhuyen.example10.security.services.CategoryService;

import java.util.List;

@Service
@AllArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    // ================= CREATE =================
    @Override
    public Category createCategory(Category category) {

        // Nếu có parent → load từ DB
        if (category.getParent() != null && category.getParent().getId() != null) {
            Category parent = categoryRepository.findById(category.getParent().getId())
                    .orElseThrow(() -> new RuntimeException(
                            "Parent category not found with id: " + category.getParent().getId()));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        return categoryRepository.save(category);
    }

    // ================= GET BY ID =================
    @Override
    public Category getCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));
    }

    // ================= GET ALL =================
    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    // ================= UPDATE =================
    @Override
    public Category updateCategory(Category category) {

        Category existingCategory = categoryRepository.findById(category.getId())
                .orElseThrow(() -> new RuntimeException(
                        "Category not found with id: " + category.getId()));

        existingCategory.setName(category.getName());
        existingCategory.setDescription(category.getDescription());
        existingCategory.setImageUrl(category.getImageUrl());
        existingCategory.setUpdatedAt(category.getUpdatedAt());

        // Update parent
        if (category.getParent() != null && category.getParent().getId() != null) {
            Category parent = categoryRepository.findById(category.getParent().getId())
                    .orElseThrow(() -> new RuntimeException(
                            "Parent category not found with id: " + category.getParent().getId()));
            existingCategory.setParent(parent);
        } else {
            existingCategory.setParent(null);
        }

        return categoryRepository.save(existingCategory);
    }

    // ================= DELETE =================
    @Override
    public void deleteCategory(Long categoryId) {
        categoryRepository.deleteById(categoryId);
    }
}
