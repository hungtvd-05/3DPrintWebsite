package com.web.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CartItemDTO {
    private Long cartId;
    private Long productId;
    private String productName;
    private String productImage;
    private Double price;
    private Integer quantity;
    private LocalDateTime updatedAt;

    public CartItemDTO(Cart cart, Product product) {
        this.cartId = cart.getId();
        this.productId = cart.getProductId();
        this.quantity = cart.getQuantity();
        this.updatedAt = cart.getUpdatedAt();

        if (product != null) {
            this.productName = product.getName();
            this.productImage = product.getImageFiles().iterator().next();
            this.price = product.getPrice();
        }
    }
}
