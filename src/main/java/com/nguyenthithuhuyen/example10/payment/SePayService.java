package com.nguyenthithuhuyen.example10.payment;

import com.nguyenthithuhuyen.example10.entity.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.nguyenthithuhuyen.example10.payload.response.QrResponse;

@Service
public class SePayService {

    @Value("${sepay.bank-code}")
    private String bankCode;

    @Value("${sepay.account-number}")
    private String accountNumber;

    @Value("${sepay.account-name}")
    private String accountName;

    public QrResponse createQr(Order order) {

        String content = order.getPaymentRef(); // nội dung chuyển khoản

        String qrUrl = String.format(
                "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%s&addInfo=%s&accountName=%s",
                bankCode,
                accountNumber,
                order.getFinalAmount().toPlainString(),
                content,
                accountName
        );

        return new QrResponse(
                qrUrl,
                content,
                order.getFinalAmount()
        );
    }
}
