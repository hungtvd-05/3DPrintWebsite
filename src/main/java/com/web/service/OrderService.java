package com.web.service;

import com.web.model.CartItemDTO;
import com.web.model.Order;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Set;

public interface OrderService {
    Order createOrderFromCart(Order order, List<CartItemDTO> cartItems);
    List<Order> getOrdersByUserId(Long userId);
    Order getOrderByOrderIdAndUserId(String orderId, Long userId);
    Order updateOrder(Order order);
    Page<Order> getAllOrdersPage(Integer pageNumber, Integer pageSize, String search);
    Order getOrderByOrderId(String orderId);
    Set<String> getAllKeywords();
}
