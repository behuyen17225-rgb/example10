package com.nguyenthithuhuyen.example10.payment;

import com.nguyenthithuhuyen.example10.entity.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.nguyenthithuhuyen.example10.payload.response.QrResponse;
import com.nguyenthithuhuyen.example10.repository.OrderRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class SePayService {
        private final OrderRepository orderRepository;


    @Value("${sepay.bank-code}")
    private String bankCode;

    @Value("${sepay.account-number}")
    private String accountNumber;

    @Value("${sepay.account-name}")
    private String accountName;

public QrResponse createQr(Order order) {

    // ✅ TẠO PAYMENT REF DUY NHẤT
    String paymentRef = "ORDER_" + order.getId();

    order.setPaymentRef(paymentRef);
    orderRepository.save(order);

    Long amount = order.getFinalAmount().longValue();

    String qrUrl =
        "https://img.vietqr.io/image/TPB-0123456789-compact2.png"
        + "?amount=" + amount
        + "&addInfo=" + paymentRef
        + "&accountName=NGUYEN%20THI%20THU%20HUYEN";

    return new QrResponse(qrUrl, paymentRef, order.getFinalAmount());
}
}