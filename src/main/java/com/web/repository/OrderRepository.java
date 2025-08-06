package com.web.repository;

import com.web.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Order findByOrderIdAndUserId(String orderId, Long userId);

    @Query("SELECT o FROM Order o order by o.updatedAt DESC")
    Page<Order> findAllOrder(Pageable pageable);

    @Query("SELECT o FROM Order o " +
            "WHERE o.orderId LIKE %:search% OR o.receiverName LIKE %:search% OR o.detailAddress LIKE %:search% OR o.phoneNumber LIKE %:search% " +
            "ORDER BY o.updatedAt DESC")
    Page<Order> findAllOrderBySearch(@Param("search") String search, Pageable pageable);

    Order findByOrderId(String orderId);

    List<Order> getOrdersByUserIdOrderByUpdatedAt(Long userId);
}
