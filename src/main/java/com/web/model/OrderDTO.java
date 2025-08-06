package com.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class OrderDTO {

    private Long id;

    private String orderId;

    private Long userId;

    private Double totalAmount;

    private String status;

    private String paymentMethod;

    private String detailAddress;

    private String receiverName;

    private String phoneNumber;

    private String note;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<OrderItemDTO> orderItems;

    public OrderDTO(Order order) {
        this.id = order.getId();
        this.orderId = order.getOrderId();
        this.userId = order.getUserId();
        this.totalAmount = order.getTotalAmount();
        this.status = order.getStatus();
        this.paymentMethod = order.getPaymentMethod();
        this.detailAddress = order.getDetailAddress();
        this.receiverName = order.getReceiverName();
        this.phoneNumber = order.getPhoneNumber();
        this.note = order.getNote();
        this.createdAt = order.getCreatedAt();
        this.updatedAt = order.getUpdatedAt();

        // Convert OrderItems to OrderItemDTOs
        if (order.getOrderItems() != null) {
            this.orderItems = order.getOrderItems().stream()
                .map(OrderItemDTO::new)
                .toList();
        }
    }


}