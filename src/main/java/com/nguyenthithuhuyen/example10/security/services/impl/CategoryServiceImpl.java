package com.nguyenthithuhuyen.example10.security.services.impl;

import com.nguyenthithuhuyen.example10.dto.CategoryDTO;
import com.nguyenthithuhuyen.example10.entity.Category;
import com.nguyenthithuhuyen.example10.repository.CategoryRepository;
import com.nguyenthithuhuyen.example10.security.services.CategoryService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public Category createCategory(Category category, Long parentId) {
        if (parentId != null) {
            Category parent = categoryRepository.findById(parentId)
                    .orElseThrow(() -> new RuntimeException("Parent category not found"));
            category.setParent(parent);
        }
        return categoryRepository.save(category);
    }

    @Override
    public Category updateCategory(Category category, Long parentId) {
        Category existing = categoryRepository.findById(category.getId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        existing.setName(category.getName());
        existing.setSlug(null); // 👈 QUAN TRỌNG
        existing.setDescription(category.getDescription());
        existing.setImageUrl(category.getImageUrl());

        if (parentId != null && !parentId.equals(existing.getId())) {
            Category parent = categoryRepository.findById(parentId)
                    .orElseThrow(() -> new RuntimeException("Parent category not found"));
            existing.setParent(parent);
        } else {
            existing.setParent(null);
        }

        return categoryRepository.save(existing);
    }

    @Override
    public CategoryDTO getCategoryById(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        return toDTO(category);
    }

    @Override
    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        if (!category.getChildren().isEmpty()) {
            throw new RuntimeException("Cannot delete category with children");
        }
        categoryRepository.delete(category);
    }

    private CategoryDTO toDTO(Category category) {
        Long parentId = category.getParent() != null
                ? category.getParent().getId()
                : null;

        String parentName = category.getParent() != null
                ? category.getParent().getName()
                : null;

        return new CategoryDTO(
                category.getId(),
                category.getName(),
                category.getSlug(),   // 👈 THÊM
                category.getDescription(),
                parentId,
                parentName
        );
    }
}
