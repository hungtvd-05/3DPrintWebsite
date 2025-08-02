package com.web.service;

import com.web.model.Comment;
import com.web.model.Product;
import com.web.model.UserAccount;

import java.util.List;

public interface CommentService {
    Comment addComment(String content, Product product, UserAccount userAccount);
    Comment addReply(Comment parentComment, String content, UserAccount userAccount);
    List<Comment> getAllCommentsOfProduct(Long id);
    void deleteByCreatedOn(Product product);
    Comment findById(Long id);
}
