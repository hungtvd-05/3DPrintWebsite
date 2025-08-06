package com.web.repository;

import com.web.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.smartcardio.Card;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserIdAndProductId(Long userId, Long productId);

    long countCartByUserId(Long userId);

    List<Cart> findByUserIdOrderByUpdatedAtDesc(Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Cart c WHERE c.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

}
