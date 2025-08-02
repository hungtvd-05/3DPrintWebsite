package com.web.repository;

import com.web.model.Notification;
import com.web.model.UserAccount;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserOrderByCreatedAtDesc(UserAccount user);
    List<Notification> findByUserAndIsReadFalseOrderByCreatedAtDesc(UserAccount user);
    long countByUserAndIsReadFalse(UserAccount user);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user = :user")
    void markAllAsReadByUser(@Param("user") UserAccount user);

    Notification findByUser_UserIdAndTypeAndSenderIdAndProductId(Long userUserId, String type, Long senderId, Long productId);

    Notification findByNotificationKey(String notificationKey);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.notificationKey = :notificationKey")
    void updateIsReadByNotificationKey(String notificationKey);
}