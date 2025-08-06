package com.web.service;

import com.web.model.Cart;
import com.web.model.CartItemDTO;

import java.util.List;

public interface CartService {
    Boolean addProductToCart(Long userId, Long productId);
    Long countCartByUserId(Long userId);
    List<CartItemDTO> getCartWithProducts(Long userId);
    Boolean updateQuantity(Long cartId, Integer newQuantity);
    Boolean deleteCartItem(Long cartId);
    Double calculateTotalPrice(List<CartItemDTO> cartItems);
    void clearCart(Long userId);
    Cart getCartByUserAndProduct(Long userId, Long productId);
    void addToCart(Cart cart);
}
