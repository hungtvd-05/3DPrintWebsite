package com.web.service.impl;

import com.web.model.Cart;
import com.web.model.CartItemDTO;
import com.web.model.Product;
import com.web.model.UserAccount;
import com.web.repository.CartRepository;
import com.web.repository.ProductRepository;
import com.web.service.CartService;
import com.web.service.ProductService;
import com.web.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private UserService userService;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Override
    public Boolean addProductToCart(Long userId, Long productId) {
        try {
            Cart item = cartRepository.findByUserIdAndProductId(userId, productId)
                    .orElse(new Cart(userId, productId));
            item.setQuantity(item.getQuantity() + 1);
            item.setUpdatedAt(LocalDateTime.now());
            cartRepository.save(item);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Long countCartByUserId(Long userId) {
        return cartRepository.countCartByUserId(userId);
    }

    @Override
    public List<CartItemDTO> getCartWithProducts(Long userId) {
        List<Cart> carts = cartRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        List<Long> productIds = carts.stream()
                .map(Cart::getProductId)
                .collect(Collectors.toList());

        Map<Long, Product> products = productRepository.findAllById(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        return carts.stream()
                .map(cart -> new CartItemDTO(
                        cart,
                        products.get(cart.getProductId())
                ))
                .collect(Collectors.toList());
    }

    @Override
    public Boolean updateQuantity(Long cartId, Integer newQuantity) {
        try {
            Optional<Cart> cartOptional = cartRepository.findById(cartId);

            if (cartOptional.isPresent()) {
                Cart cart = cartOptional.get();

                // Validate quantity
                if (newQuantity < 1) {
                    newQuantity = 1;
                } else if (newQuantity > 99) {
                    newQuantity = 99;
                }

                cart.setQuantity(newQuantity);
                cartRepository.save(cart);
                return true;
            }

            return false;

        } catch (Exception e) {
            throw new RuntimeException("Failed to update cart quantity", e);
        }
    }

    @Override
    public Boolean deleteCartItem(Long cartId) {
        try {
            cartRepository.deleteById(cartId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
