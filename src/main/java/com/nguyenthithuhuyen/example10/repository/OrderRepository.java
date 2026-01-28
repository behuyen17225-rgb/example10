package com.nguyenthithuhuyen.example10.repository;

import com.nguyenthithuhuyen.example10.entity.Order;
import com.nguyenthithuhuyen.example10.entity.User;
import com.nguyenthithuhuyen.example10.entity.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import com.nguyenthithuhuyen.example10.entity.TableEntity;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.repository.query.Param;


import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUser(User user);
    List<Order> findByStatus(OrderStatus status);
    Optional<Order> findByPaymentRef(String paymentRef);
    List<Order> findByUser_Username(String username);

    @Query("""
        SELECT p.id, p.name, SUM(oi.quantity)
        FROM OrderItem oi
        JOIN oi.order o
        JOIN oi.product p
        WHERE o.status = :status
        GROUP BY p.id, p.name
        ORDER BY SUM(oi.quantity) DESC
    """)
    List<Object[]> findTopSellingProducts(
            @Param("status") OrderStatus status,
            Pageable pageable
    );

    @Query("""
        SELECT c.name, SUM(oi.subtotal)
        FROM OrderItem oi
        JOIN oi.order o
        JOIN oi.product p
        JOIN p.category c
        WHERE o.status = :status
        GROUP BY c.name
    """)
    List<Object[]> findRevenueByCategory(
            @Param("status") OrderStatus status
    );

    @Query("""
        SELECT FUNCTION('DATE', o.createdAt), SUM(o.finalAmount)
        FROM Order o
        WHERE o.status = :status
        GROUP BY FUNCTION('DATE', o.createdAt)
        ORDER BY FUNCTION('DATE', o.createdAt)
    """)
    List<Object[]> findRevenueByDay(
            @Param("status") OrderStatus status
    );

    Optional<Order> findFirstByTableIdAndStatusIn(
            Long tableId,
            List<OrderStatus> statuses
    );
}
