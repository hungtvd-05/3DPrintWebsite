package com.web.repository;

import com.web.model.Comment;
import com.web.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Query("SELECT c FROM Comment c WHERE c.product.id = :productId AND c.parentComment IS NULL ORDER BY c.updatedAt DESC")
    List<Comment> findAllCommentsOfProduct(Long productId);

    void deleteByProduct(Product product);
}
