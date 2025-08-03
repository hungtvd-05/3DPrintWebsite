package com.web.service;

import com.web.model.CartItemDTO;
import com.web.model.Product;
import com.web.model.UserAccount;

import java.util.List;

public interface CartService {
    Boolean addProductToCart(Long userId, Long productId);
    Long countCartByUserId(Long userId);
    List<CartItemDTO> getCartWithProducts(Long userId);
    Boolean updateQuantity(Long cartId, Integer newQuantity);
    Boolean deleteCartItem(Long cartId);
}
