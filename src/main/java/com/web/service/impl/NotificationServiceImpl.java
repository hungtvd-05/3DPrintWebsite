package com.web.service.impl;

import com.web.model.Notification;
import com.web.model.Product;
import com.web.model.UserAccount;
import com.web.repository.NotificationRepository;
import com.web.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Override
    public void saveAndSendNotification(UserAccount recipient, String type, String message, Product product, UserAccount sender, LocalDateTime createdAt, String notificationKey) {

        Notification notification = notificationRepository.findByNotificationKey(notificationKey);

        if (ObjectUtils.isEmpty(notification)) {
            notification = new Notification();
            notification.setUser(recipient);
            notification.setType(type);
            notification.setContent(message);
            notification.setProductId(product.getId());
            notification.setSenderId(sender.getUserId());
            notification.setSenderName(sender.getFullName());
            notification.setSenderAvatar(sender.getProfileImage());
            notification.setCreatedAt(createdAt);
            notification.setNotificationKey(notificationKey);
        } else {
            notification.setCreatedAt(createdAt);
            notification.setIsRead(false);
            notification.setContent(message);
            notification.setSenderName(sender.getFullName());
            notification.setSenderAvatar(sender.getProfileImage());
        }

        notificationRepository.save(notification);

    }

    @Override
    public List<Notification> getUserNotifications(UserAccount user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Override
    public long getUnreadCount(UserAccount user) {
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    @Override
    public void markAllAsRead(UserAccount user) {
        notificationRepository.markAllAsReadByUser(user);
    }

    @Override
    public void markAsRead(String notificationKey) {
        notificationRepository.updateIsReadByNotificationKey(notificationKey);
    }
}
