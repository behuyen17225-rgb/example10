package com.nguyenthithuhuyen.example10.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.nguyenthithuhuyen.example10.entity.Product;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByIsActiveTrue();

    List<Product> findByNameContainingIgnoreCase(String keyword);

    List<Product> findTop5ByNameContainingIgnoreCase(String name);

    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.category WHERE p.id = :id")
    Optional<Product> findByIdWithCategory(@Param("id") Long id);

@Query("""
SELECT DISTINCT p FROM Product p
LEFT JOIN p.prices pr
WHERE p.isActive = true
AND (:keyword IS NULL OR lower(p.name) LIKE lower(concat('%', :keyword, '%')))
AND (:maxPrice IS NULL OR pr.price <= :maxPrice)
ORDER BY pr.price ASC
""")
    List<Product> searchByChat(
            @Param("keyword") String keyword,
            @Param("maxPrice") Integer maxPrice,
            Pageable pageable);

    @Query("""
            SELECT p FROM Product p
            WHERE LOWER(p.name) LIKE %:kw%
               OR LOWER(p.description) LIKE %:kw%
            """)
    List<Product> search(@Param("kw") String kw, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.category")
    List<Product> findAllWithCategory();
    // Tìm kiếm sản phẩm còn hoạt động (Hiển thị cho khách hàng)

    // Tìm kiếm sản phẩm theo Category và còn hoạt động
    List<Product> findByCategory_IdAndIsActiveTrue(Long categoryId);

    // Tìm kiếm sản phẩm theo tên (cho Nhân viên/Khách hàng tìm kiếm)
    List<Product> findByNameContainingIgnoreCaseAndIsActiveTrue(String name);
}
