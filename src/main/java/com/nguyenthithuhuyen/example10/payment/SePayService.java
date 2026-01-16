package com.nguyenthithuhuyen.example10.payment;

import com.nguyenthithuhuyen.example10.entity.Order;
import com.nguyenthithuhuyen.example10.payload.response.QrResponse;
import com.nguyenthithuhuyen.example10.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class SePayService {

    private static final Logger log =
            LoggerFactory.getLogger(SePayService.class);

    private final OrderRepository orderRepository;

    @Value("${sepay.bank-code}")
    private String bankCode;          // TPB

    @Value("${sepay.account-number}")
    private String accountNumber;

    @Value("${sepay.account-name}")
    private String accountName;

    public QrResponse createQr(Order order) {

        log.info("===== CREATE VIETQR =====");
        log.info("Order ID      : {}", order.getId());
        log.info("Final amount  : {}", order.getFinalAmount());
        log.info("Payment ref   : {}", order.getPaymentRef());

        // ===== VALIDATE =====
        if (order.getFinalAmount() == null) {
            throw new RuntimeException("finalAmount is null");
        }

        long amount;
        try {
            amount = order.getFinalAmount().longValueExact();
        } catch (ArithmeticException e) {
            throw new RuntimeException("Amount must be integer VND", e);
        }

        if (amount <= 0) {
            throw new RuntimeException("Amount must be > 0");
        }

        // ===== PAYMENT REF (Đã được set trong createOrder) =====
        if (order.getPaymentRef() == null || order.getPaymentRef().isBlank()) {
            log.error("❌ Payment ref not set! Order ID: {}", order.getId());
            throw new RuntimeException("Payment ref must be set before creating QR");
        }

        log.info("Using paymentRef: {}", order.getPaymentRef());

        String encodedName =
                URLEncoder.encode(accountName, StandardCharsets.UTF_8);

        String qrUrl =
                "https://img.vietqr.io/image/"
                        + bankCode + "-" + accountNumber + "-compact2.png"
                        + "?amount=" + amount
                        + "&addInfo=" + order.getPaymentRef()
                        + "&accountName=" + encodedName;

        log.info("VietQR URL: {}", qrUrl);
        log.info("===== END CREATE VIETQR =====");

        return new QrResponse(
                qrUrl,
                order.getPaymentRef(),
                order.getFinalAmount()
        );
    }
}
