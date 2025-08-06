package com.web.util;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.CompletableFuture;

@Component
public class CommonUtil {

    @Autowired
    private JavaMailSender mailSender;

    @Async
    public CompletableFuture<Boolean> sendConfirmEmail(String url, String email) {

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("hungtvd.26@gmail.com", "Shoping cart");
            helper.setTo(email);

            String content = "<p>Hello.</p>" +
                    "<p>Bạn đã yêu cầu đăng ký tài khoản.</p>" +
                    "<p>Hãy bấm vào link này để xác nhận tạo tài khoản: </p>" +
                    "<p><a href = \"" + url + "\">Link xác nhận tài khoản</a></p>";

            helper.setSubject("Xác nhận tài khoản");
            helper.setText(content, true);
            mailSender.send(mimeMessage);

            return CompletableFuture.completedFuture(true);
        }  catch (Exception e) {
            return CompletableFuture.completedFuture(false);
        }
    }

    @Async
    public CompletableFuture<Boolean> sendOrderConfirmationEmailAsync(
            String toEmail,
            String customerName,
            String orderDetails) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("hungtvd.26@gmail.com", "Shoping cart");
            helper.setTo(toEmail);
            helper.setSubject("Xác nhận đơn hàng");

            String emailContent = buildOrderEmailTemplate(customerName, orderDetails);
            helper.setText(emailContent, true);

            mailSender.send(message);
            return CompletableFuture.completedFuture(true);

        } catch (Exception e) {
            return CompletableFuture.completedFuture(false);
        }
    }

    public String buildOrderEmailTemplate(String customerName, String orderDetails) {
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

    @Async
    public CompletableFuture<Boolean> sendMail(String url, String email) {

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);

            helper.setFrom("hungtvd.26@gmail.com", "Shoping cart");
            helper.setTo(email);

            String content = "<p>Hello.</p>" +
                    "<p>Bạn đã yêu cầu reset mật khẩu.</p>" +
                    "<p>Hãy bấm vào link này để tiếp tục: </p>" +
                    "<p><a href = \"" + url + "\">Thay đổi mật khẩu</a></p>";

            helper.setSubject("Reset mật khẩu");
            helper.setText(content, true);
            mailSender.send(mimeMessage);

            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            return CompletableFuture.completedFuture(false);
        }
    }

    public static String generateUrl(HttpServletRequest request) {
        String siteUrl = request.getRequestURL().toString();

        return siteUrl.replace(request.getServletPath(), "");
    }

    public static String formatPrice(Number price) {
        if (price == null) {
            return "0";
        }

        return String.format("%,.0f", price.doubleValue());
    }

    @Async
    public CompletableFuture<Boolean> sendNewOrderNotificationToAdmin(
            String adminEmail,
            String customerName,
            String customerEmail,
            String orderId,
            Double totalAmount,
            String orderDetails) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("hungtvd.26@gmail.com", "Shopping Cart System");
            helper.setTo(adminEmail);
            helper.setSubject("🔔 Đơn hàng mới - " + orderId);

            String emailContent = buildAdminOrderNotificationTemplate(
                    customerName, customerEmail, orderId, totalAmount, orderDetails);
            helper.setText(emailContent, true);

            mailSender.send(message);
            return CompletableFuture.completedFuture(true);

        } catch (Exception e) {
            System.err.println("Error sending admin notification: " + e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    public String buildAdminOrderNotificationTemplate(
            String customerName,
            String customerEmail,
            String orderId,
            Double totalAmount,
            String orderDetails) {
        return String.format("""
        <html>
        <body style="font-family: Arial, sans-serif; margin: 20px;">
            <div style="background-color: #f8f9fa; padding: 20px; border-radius: 8px;">
                <h2 style="color: #28a745; margin-bottom: 20px;">🔔 Thông báo đơn hàng mới</h2>
                
                <div style="background-color: white; padding: 15px; border-radius: 5px; margin-bottom: 15px;">
                    <h3 style="color: #333; margin-top: 0;">Thông tin khách hàng</h3>
                    <p><strong>Tên khách hàng:</strong> %s</p>
                    <p><strong>Email:</strong> %s</p>
                </div>
                
                <div style="background-color: white; padding: 15px; border-radius: 5px; margin-bottom: 15px;">
                    <h3 style="color: #333; margin-top: 0;">Thông tin đơn hàng</h3>
                    <p><strong>Mã đơn hàng:</strong> <span style="color: #007bff;">%s</span></p>
                    <p><strong>Tổng tiền:</strong> <span style="color: #dc3545; font-size: 18px; font-weight: bold;">%s VNĐ</span></p>
                </div>
                
                <div style="background-color: white; padding: 15px; border-radius: 5px;">
                    <h3 style="color: #333; margin-top: 0;">Chi tiết đơn hàng</h3>
                    %s
                </div>
                
                <div style="margin-top: 20px; text-align: center;">
                    <p style="color: #6c757d; font-style: italic;">
                        Vui lòng kiểm tra và xử lý đơn hàng trong admin panel.
                    </p>
                </div>
            </div>
        </body>
        </html>
        """, customerName, customerEmail, orderId, CommonUtil.formatPrice(totalAmount), orderDetails);
    }

    @Async
    public CompletableFuture<Boolean> sendOrderCancellationEmailAsync(
            String toEmail,
            String customerName,
            String orderId) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("hungtvd.26@gmail.com", "Shopping Cart");
            helper.setTo(toEmail);
            helper.setSubject("Thông báo hủy đơn hàng - " + orderId);

            String emailContent = buildOrderCancellationEmailTemplate(customerName, orderId);
            helper.setText(emailContent, true);

            mailSender.send(message);
            return CompletableFuture.completedFuture(true);

        } catch (Exception e) {
            System.err.println("Error sending order cancellation email: " + e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    public String buildOrderCancellationEmailTemplate(String customerName, String orderId) {
        return String.format("""
    <html>
    <body style="font-family: Arial, sans-serif; margin: 20px;">
        <div style="background-color: #f8f9fa; padding: 20px; border-radius: 8px;">
            <h2 style="color: #dc3545; margin-bottom: 20px;">❌ Thông báo hủy đơn hàng</h2>
            
            <div style="background-color: white; padding: 20px; border-radius: 5px; margin-bottom: 15px;">
                <p style="font-size: 16px; margin-bottom: 15px;">Xin chào <strong>%s</strong>,</p>
                
                <p style="margin-bottom: 15px;">
                    Đơn hàng <strong style="color: #007bff;">#%s</strong> của bạn đã được hủy thành công.
                </p>
                
                <div style="background-color: #fff3cd; border: 1px solid #ffeaa7; padding: 15px; border-radius: 5px; margin: 15px 0;">
                    <h4 style="color: #856404; margin-top: 0;">📋 Thông tin hủy đơn:</h4>
                    <ul style="color: #856404; margin-bottom: 0;">
                        <li>Thời gian hủy: %s</li>
                        <li>Trạng thái: <strong>Đã hủy</strong></li>
                    </ul>
                </div>
                
                <p style="margin-bottom: 15px;">
                    Nếu bạn có bất kỳ thắc mắc nào về việc hủy đơn hàng, vui lòng liên hệ với chúng tôi.
                </p>
                
                <p style="margin-bottom: 15px;">
                    Cảm ơn bạn đã tin tưởng và sử dụng dịch vụ của chúng tôi!
                </p>
            </div>
            
            <div style="text-align: center; margin-top: 20px;">
                <div style="background-color: #e9ecef; padding: 15px; border-radius: 5px;">
                    <p style="margin: 0; color: #6c757d; font-size: 14px;">
                        📞 Hotline: 1900-xxxx | 📧 Email: support@shoppingcart.com
                    </p>
                    <p style="margin: 5px 0 0 0; color: #6c757d; font-size: 14px;">
                        🌐 Website: www.shoppingcart.com
                    </p>
                </div>
            </div>
        </div>
    </body>
    </html>
    """, customerName, orderId, new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date()));
    }

    @Async
    public CompletableFuture<Boolean> sendOrderCancellationNotificationToAdmin(
            String adminEmail,
            String customerName,
            String customerEmail,
            String orderId,
            Double totalAmount,
            String cancellationReason) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("hungtvd.26@gmail.com", "Shopping Cart System");
            helper.setTo(adminEmail);
            helper.setSubject("⚠️ Thông báo hủy đơn hàng - " + orderId);

            String emailContent = buildAdminCancellationNotificationTemplate(
                    customerName, customerEmail, orderId, totalAmount, cancellationReason);
            helper.setText(emailContent, true);

            mailSender.send(message);
            return CompletableFuture.completedFuture(true);

        } catch (Exception e) {
            System.err.println("Error sending admin cancellation notification: " + e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    public String buildAdminCancellationNotificationTemplate(
            String customerName,
            String customerEmail,
            String orderId,
            Double totalAmount,
            String cancellationReason) {
        return String.format("""
    <html>
    <body style="font-family: Arial, sans-serif; margin: 20px;">
        <div style="background-color: #f8f9fa; padding: 20px; border-radius: 8px;">
            <h2 style="color: #dc3545; margin-bottom: 20px;">⚠️ Thông báo hủy đơn hàng</h2>

            <div style="background-color: white; padding: 15px; border-radius: 5px; margin-bottom: 15px;">
                <h3 style="color: #333; margin-top: 0;">Thông tin khách hàng</h3>
                <p><strong>Tên khách hàng:</strong> %s</p>
                <p><strong>Email:</strong> %s</p>
            </div>

            <div style="background-color: white; padding: 15px; border-radius: 5px; margin-bottom: 15px;">
                <h3 style="color: #333; margin-top: 0;">Thông tin đơn hàng bị hủy</h3>
                <p><strong>Mã đơn hàng:</strong> <span style="color: #007bff;">%s</span></p>
                <p><strong>Tổng tiền:</strong> <span style="color: #dc3545; font-size: 18px; font-weight: bold;">%s VNĐ</span></p>
                <p><strong>Thời gian hủy:</strong> %s</p>
                <p><strong>Lý do hủy:</strong> %s</p>
            </div>

            <div style="background-color: #f8d7da; border: 1px solid #f5c6cb; padding: 15px; border-radius: 5px; margin: 15px 0;">
                <h4 style="color: #721c24; margin-top: 0;">📊 Tác động:</h4>
                <ul style="color: #721c24; margin-bottom: 0;">
                    <li>Doanh thu bị mất: <strong>%s VNĐ</strong></li>
                </ul>
            </div>

            <div style="text-align: center; margin-top: 20px;">
                <div style="background-color: #e9ecef; padding: 15px; border-radius: 5px;">
                    <p style="margin: 0; color: #6c757d; font-size: 14px;">
                        🔔 Hệ thống thông báo tự động - Shopping Cart Admin
                    </p>
                    <p style="margin: 5px 0 0 0; color: #6c757d; font-size: 14px;">
                        📅 Thời gian gửi: %s
                    </p>
                </div>
            </div>
        </div>
    </body>
    </html>
    """,
                customerName,
                customerEmail,
                orderId,
                CommonUtil.formatPrice(totalAmount),
                new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date()),
                cancellationReason != null ? cancellationReason : "Khách hàng hủy đơn",
                CommonUtil.formatPrice(totalAmount),
                new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date()));
    }
}
