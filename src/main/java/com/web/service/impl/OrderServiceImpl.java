package com.web.service.impl;

import com.web.model.CartItemDTO;
import com.web.model.Order;
import com.web.model.OrderItem;
import com.web.repository.OrderRepository;
import com.web.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Override
    @Transactional
    public Order createOrderFromCart(Order order, List<CartItemDTO> cartItems) {

        try {
            List<OrderItem> orderItems = new ArrayList<>();
            for (CartItemDTO cartItem : cartItems) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrderId(order.getOrderId());
                orderItem.setProductId(cartItem.getProductId());
                orderItem.setQuantity(cartItem.getQuantity());
                orderItem.setPrice(cartItem.getPrice());
                orderItems.add(orderItem);
            }

            order.setOrderItems(orderItems);

            return orderRepository.save(order);
        } catch (Exception e) {
            return null;
        }

    }

    @Override
    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.getOrdersByUserIdOrderByUpdatedAt(userId);
    }

    @Override
    public Order getOrderByOrderIdAndUserId(String orderId, Long userId) {
        return orderRepository.findByOrderIdAndUserId(orderId, userId);
    }

    @Override
    public Order updateOrder(Order order) {
        return orderRepository.save(order);
    }

    @Override
    public Page<Order> getAllOrdersPage(Integer pageNumber, Integer pageSize, String search) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        if (search != null && !search.isEmpty()) {
            return orderRepository.findAllOrderBySearch(search, pageable);
        }
        return orderRepository.findAllOrder(pageable);
    }

    @Override
    public Order getOrderByOrderId(String orderId) {
        return orderRepository.findByOrderId(orderId);
    }

    @Override
    public Set<String> getAllKeywords() {
        List<Order> orders = orderRepository.findAll();
        return orders.stream()
                .flatMap(order -> Stream.of(
                        order.getOrderId(),
                        order.getReceiverName(),
                        order.getPhoneNumber(),
                        order.getDetailAddress()
                ))
                .filter(keyword -> keyword != null && !keyword.trim().isEmpty())
                .collect(Collectors.toSet());
    }

}
