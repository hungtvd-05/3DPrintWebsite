package com.web.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user; // Người nhận thông báo

    @Column(nullable = false)
    private String type; // new_comment, new_order, etc.

    @Column(nullable = false, length = 1000)
    private String content; // Nội dung thông báo

    @Column(name = "product_id")
    private String contentId;

    @Column(name = "sender_id")
    private Long senderId; // Người gửi thông báo

    @Column(name = "sender_name")
    private String senderName;

    @Column(name = "sender_avatar")
    private String senderAvatar;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "notification_key", nullable = false, unique = true)
    private String notificationKey;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
