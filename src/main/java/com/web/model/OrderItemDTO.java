package com.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class OrderItemDTO {
    private Long id;
    private String orderId;
    private Long productId;
    private Integer quantity;
    private Double price;
    private LocalDateTime createdAt;
    private String productName;

    public OrderItemDTO(OrderItem orderItem) {
        this.id = orderItem.getId();
        this.orderId = orderItem.getOrderId();
        this.productId = orderItem.getProductId();
        this.quantity = orderItem.getQuantity();
        this.price = orderItem.getPrice();
        this.createdAt = orderItem.getCreatedAt();

        // Lấy tên sản phẩm từ product relationship
        if (orderItem.getProduct() != null) {
            this.productName = orderItem.getProduct().getName();
        }
    }
}
