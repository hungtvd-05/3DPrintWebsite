package com.web.service;

import com.web.model.Order;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;

public interface CheckOutService {

    @Transactional
    String checkOutWithPayOnline(Order order, String urlReturn);

    int orderReturn(HttpServletRequest request, Order order) throws MessagingException, UnsupportedEncodingException;

}
