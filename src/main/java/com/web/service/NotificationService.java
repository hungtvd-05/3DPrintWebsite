package com.web.service;

import com.web.model.Notification;
import com.web.model.Product;
import com.web.model.UserAccount;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationService {
    void saveAndSendNotification(UserAccount recipient, String type,
                                 String message, Product product, UserAccount sender, LocalDateTime createdAt, String notificationKey);
    List<Notification> getUserNotifications(UserAccount user);
    long getUnreadCount(UserAccount user);
    void markAllAsRead(UserAccount user);
    void markAsRead(String notificationKey);
}
