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

import javax.smartcardio.Card;
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
                .filter(product -> !product.getIsDeleted() && product.getStatus() && product.getConfirmed() == 1)
                .collect(Collectors.toMap(Product::getId, p -> p));

        // Tách cart items thành hợp lệ và không hợp lệ
        Map<Boolean, List<Cart>> partitionedCarts = carts.stream()
                .collect(Collectors.partitioningBy(
                        cart -> products.containsKey(cart.getProductId())
                ));

        List<Cart> validCarts = partitionedCarts.get(true);
        List<Cart> invalidCarts = partitionedCarts.get(false);

        // Batch delete các cart items không hợp lệ
        if (!invalidCarts.isEmpty()) {
            List<Long> invalidCartIds = invalidCarts.stream()
                    .map(Cart::getId)
                    .collect(Collectors.toList());
            cartRepository.deleteAllById(invalidCartIds);
        }

        // Trả về các cart items hợp lệ
        return validCarts.stream()
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

    @Override
    public Double calculateTotalPrice(List<CartItemDTO> cartItems) {

        return cartItems.stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();
    }

    @Override
    public void clearCart(Long userId) {
        cartRepository.deleteAllByUserId(userId);
    }

    @Override
    public Cart getCartByUserAndProduct(Long userId, Long productId) {
        return cartRepository.findByUserIdAndProductId(userId, productId)
                .orElse(null);
    }

    @Override
    public void addToCart(Cart cart) {
        cartRepository.save(cart);
    }
}
