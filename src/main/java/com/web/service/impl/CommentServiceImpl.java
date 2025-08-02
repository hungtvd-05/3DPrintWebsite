package com.web.service.impl;

import com.web.model.Comment;
import com.web.model.Product;
import com.web.model.UserAccount;
import com.web.repository.CommentRepository;
import com.web.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Override
    @Transactional
    public Comment addComment(String content, Product product, UserAccount userAccount) {
        Comment comment = new Comment();
        comment.setContent(content);
        comment.setUserAccount(userAccount);
        comment.setProduct(product);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(comment.getCreatedAt());
        return commentRepository.save(comment);
    }

    @Override
    @Transactional
    public Comment addReply(Comment parentComment, String content, UserAccount userAccount) {

        Comment reply = new Comment();
        reply.setContent(content);
        reply.setUserAccount(userAccount);
        reply.setParentComment(parentComment);
        reply.setProduct(parentComment.getProduct());
        reply.setCreatedAt(LocalDateTime.now());
        reply.setUpdatedAt(reply.getCreatedAt());

        parentComment.setUpdatedAt(reply.getCreatedAt());
        parentComment.getReplies().add(reply);

        return commentRepository.save(reply);
    }

    @Override
    public List<Comment> getAllCommentsOfProduct(Long id) {
        return commentRepository.findAllCommentsOfProduct(id);
    }

    @Override
    @Transactional
    public void deleteByCreatedOn(Product product) {
        commentRepository.deleteByProduct(product);
    }

    @Override
    public Comment findById(Long id) {
        return commentRepository.findById(id).orElse(null);
    }


}
