package com.web.service.impl;

import com.web.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class EmailServiceImpl implements EmailService {
    @Autowired
    private JavaMailSender mailSender;

    @Async
    @Override
    public CompletableFuture<Boolean> sendOrderConfirmationEmailAsync(
            String toEmail,
            String customerName,
            String orderDetails) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Xác nhận đơn hàng - " + orderDetails);

            String emailContent = buildOrderEmailTemplate(customerName, orderDetails);
            helper.setText(emailContent, true);

            mailSender.send(message);
            return CompletableFuture.completedFuture(true);

        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(false);
        }
    }

    private String buildOrderEmailTemplate(String customerName, String orderDetails) {
        return String.format("""
            <html>
            <body>
                <h2>Xác nhận đơn hàng</h2>
                <p>Xin chào %s,</p>
                <p>Cảm ơn bạn đã đặt hàng. Chi tiết đơn hàng:</p>
                <div>%s</div>
                <p>Chúng tôi sẽ liên hệ với bạn sớm nhất!</p>
            </body>
            </html>
            """, customerName, orderDetails);
    }
}
