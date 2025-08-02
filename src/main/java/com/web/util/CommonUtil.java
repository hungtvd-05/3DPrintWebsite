package com.web.util;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;

@Component
public class CommonUtil {

    @Autowired
    private JavaMailSender mailSender;

    public Boolean sendConfirmEmail(String url, String email) throws MessagingException, UnsupportedEncodingException {

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);

        helper.setFrom("hungtvd.26@gmail.com", "Shoping cart");
        helper.setTo(email);

        String content = "<p>Hello.</p>" +
                "<p>Bạn đã yêu cầu đăng ký tài khoản.</p>" +
                "<p>Hãy bấm vào link này để xác nhận tạo tài khoản: </p>" +
                "<p><a href = \"" + url + "\">Link xác nhận tài khoản</a></p>";

        helper.setSubject("Xác nhận tài khoản");
        helper.setText(content, true);
        mailSender.send(mimeMessage);

        return true;
    }

    public Boolean sendMail(String url, String email) throws MessagingException, UnsupportedEncodingException {

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

        return true;
    }

    public static String generateUrl(HttpServletRequest request) {
        String siteUrl = request.getRequestURL().toString();

        return siteUrl.replace(request.getServletPath(), "");
    }

//    public Boolean sendMailForOrder(Orders order, String status) throws MessagingException, UnsupportedEncodingException {
//
//        MimeMessage mimeMessage = mailSender.createMimeMessage();
//        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
//
//        helper.setFrom("hungtvd.26@gmail.com", "Shoping cart");
//        helper.setTo(order.getUser().getEmail());
//
//        String msg = "<p>Cảm ơn: "+ status +"</p>" +
//                "<p>Mã đơn hàng: "+ order.getOrderId() +"</p>" +
//                "<p>Tên người nhận: "+ order.getOrderAddress().getFullName() +"</p>" +
//                "<p>Số điện thoại: "+ order.getOrderAddress().getPhoneNumber() +"</p>" +
//                "<p>Địa chỉ giao hàng: "+ order.getFullAdressFormatted() +"</p>" +
//                "<p>Tên sản phẩm: " + order.getOrderName() + "</p>" +
//                "<p>Tổng giá: " + order.getTotalPriceFormatted() + "</p>" +
//                "<p>Phương thức thanh toán: " + order.getPaymentType() + "</p>";
//
//        helper.setSubject("Thông tin đơn hàng!");
//        helper.setText(msg, true);
//        mailSender.send(mimeMessage);
//
//        return true;
//    }
}
