package com.web.repository;

import com.web.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserIdAndProductId(Long userId, Long productId);

    long countCartByUserId(Long userId);

    List<Cart> findByUserId(Long userId);

    List<Cart> findByUserIdOrderByUpdatedAtDesc(Long userId);
//    List<Cart> findByUserUserIdOrderByCreatedAtDesc(Long userId);
//
//    Optional<Cart> findByUserUserIdAndProductId(Long userId, Long productId);
//
//    void deleteByUserUserIdAndProductId(Long userId, Long productId);
//
//    @Query("SELECT SUM(c.quantity) FROM Cart c WHERE c.user.userId = :userId")
//    Integer countItemsByUserId(@Param("userId") Long userId);
//
//    @Query("SELECT SUM(c.quantity * c.product.price) FROM Cart c WHERE c.user.userId = :userId")
//    Double calculateTotalByUserId(@Param("userId") Long userId);
//
//    ScopedValue<Object> findByUserIdAndProductId(Long userId, Long productId);
}
