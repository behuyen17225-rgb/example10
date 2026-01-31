package com.nguyenthithuhuyen.example10.service;

import com.nguyenthithuhuyen.example10.dto.OrderResponse;
import com.nguyenthithuhuyen.example10.entity.Order;
import com.nguyenthithuhuyen.example10.entity.TableEntity;
import com.nguyenthithuhuyen.example10.entity.enums.OrderStatus;
import com.nguyenthithuhuyen.example10.entity.enums.OrderType;
import com.nguyenthithuhuyen.example10.repository.*;
import com.nguyenthithuhuyen.example10.security.services.OrderService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private TableRepository tableRepository;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private OrderService orderService;

    /* ================= TEST GET ALL ORDERS ================= */
    @Test
    void testGetAllOrderResponses() {

        Order order1 = mockOrder(1L);
        Order order2 = mockOrder(2L);

        when(orderRepository.findAll()).thenReturn(List.of(order1, order2));

        List<OrderResponse> result = orderService.getAllOrderResponses();

        assertEquals(2, result.size());
        verify(orderRepository, times(1)).findAll();
    }

    /* ================= TEST GET ORDER BY ID ================= */
    @Test
    void testGetOrderById_success() {

        Order order = mockOrder(1L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        Order result = orderService.getOrderById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    /* ================= TEST GET ORDER BY ID - NOT FOUND ================= */
    @Test
    void testGetOrderById_notFound() {

        when(orderRepository.findById(99L))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> orderService.getOrderById(99L)
        );

        assertEquals("Order not found", ex.getMessage());
    }

    /* ================= MOCK ORDER ================= */
    private Order mockOrder(Long id) {

        TableEntity table = new TableEntity();
        table.setId(1L);

        Order order = new Order();
        order.setId(id);
        order.setCustomerName("Test");
        order.setPhone("0123456789");
        order.setTable(table);
        order.setStatus(OrderStatus.PAID);
        order.setOrderType(OrderType.DINE_IN);
        order.setPaymentMethod("CASH");
        order.setTotalAmount(BigDecimal.valueOf(100000));
        order.setFinalAmount(BigDecimal.valueOf(100000));
        order.setCreatedAt(LocalDateTime.now());
        order.setOrderItems(List.of()); // QUAN TRỌNG: tránh NPE

        return order;
    }
}
