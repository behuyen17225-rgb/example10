package com.nguyenthithuhuyen.example10.config;

import com.nguyenthithuhuyen.example10.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataMigrationRunner implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    @Override
    public void run(String... args) {
        categoryRepository.findAll().forEach(category -> {
            if (category.getSlug() == null || category.getSlug().isBlank()) {
                category.setSlug(null); // trigger @PreUpdate
                categoryRepository.save(category);
            }
        });

        System.out.println("✅ Category slug migration done");
    }
}

