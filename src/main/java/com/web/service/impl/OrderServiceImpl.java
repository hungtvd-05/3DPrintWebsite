package com.web.service.impl;

import com.web.model.CartItemDTO;
import com.web.model.Order;
import com.web.model.OrderItem;
import com.web.repository.OrderItemRepository;
import com.web.repository.OrderRepository;
import com.web.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
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

    @Autowired
    private OrderItemRepository orderItemRepository;

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

            System.out.println("Order created at: " + order.getCreatedAt());
            order.setIsPaid(false);
            if (order.getPaymentMethod().equals("ONLINE")) {
                order.setIsCreated(false);
            } else {
                order.setIsCreated(true);
            }
            System.out.println("Order isCreated: " + order.getIsCreated());

            return orderRepository.save(order);
        } catch (Exception e) {
            System.out.println("Error creating order: " + e.getMessage());
            return null;
        }

    }

    @Override
    public List<Order> getOrdersByUserId(Long userId) {
//        return orderRepository.getOrdersByUserIdOrderByUpdatedAt(userId);
        return orderRepository.getOrdersByUserIdAndIsCreatedOrderByUpdatedAt(userId, true);
    }

    @Override
    public Order getOrderByOrderIdAndUserId(String orderId, Long userId) {
        return orderRepository.findByOrderIdAndUserIdAndIsCreated(orderId, userId, true);
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
//        return orderRepository.findByOrderId(orderId);
        return orderRepository.findByOrderIdAndIsCreated(orderId, true);
    }

    @Override
    public Set<String> getAllKeywords() {
        List<Order> orders = orderRepository.getOrdersByIsCreated(true);
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

    @Override
    public Order saveOrder(Order order) {
        if (order.getPaymentMethod().equals("ONLINE")) {
            order.setIsCreated(true);
            order.setIsPaid(true);
        } else {
            order.setIsPaid(false);
        }
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Boolean deleteOrder(Order order) {
        if (!ObjectUtils.isEmpty(order)) {
            orderItemRepository.deleteByOrderId(order.getOrderId());
            orderRepository.delete(order);
            return true;
        }

        return false;

    }

    @Override
    public Order getOrderById(String orderId) {
        return orderRepository.findByOrderId(orderId);
    }

}
