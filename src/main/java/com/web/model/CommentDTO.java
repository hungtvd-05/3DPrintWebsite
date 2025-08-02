package com.web.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommentDTO {
    private Long parentCommentId;
    private String content;
    private UserAccount userAccount;
    private Long productId;
    private LocalDateTime createdAt;
}
