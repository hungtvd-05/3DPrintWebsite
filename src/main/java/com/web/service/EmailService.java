package com.web.service;

import java.util.concurrent.CompletableFuture;

public interface EmailService {
    CompletableFuture<Boolean> sendOrderConfirmationEmailAsync(
            String toEmail,
            String customerName,
            String orderDetails);
}
