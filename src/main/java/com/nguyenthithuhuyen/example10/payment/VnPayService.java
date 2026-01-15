package com.nguyenthithuhuyen.example10.payment;

import com.nguyenthithuhuyen.example10.entity.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.*;


@Service
public class VnPayService {

    @Value("${vnpay.tmnCode}")
    private String tmnCode;

    @Value("${vnpay.hashSecret}")
    private String hashSecret;

    @Value("${vnpay.payUrl}")
    private String payUrl;

    @Value("${vnpay.returnUrl}")
    private String returnUrl;

    public String createPaymentUrl(Order order) {

        Map<String, String> params = new HashMap<>();

        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", tmnCode);
        params.put("vnp_Amount",
                order.getFinalAmount()
                        .multiply(BigDecimal.valueOf(100))
                        .toBigInteger()
                        .toString()
        );
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", String.valueOf(order.getId()));
        params.put("vnp_OrderInfo", "Thanh toan don hang " + order.getId());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", returnUrl);
        params.put("vnp_IpAddr", "127.0.0.1");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        params.put("vnp_CreateDate", LocalDateTime.now().format(formatter));

        // SORT PARAMS
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (String field : fieldNames) {
            String value = params.get(field);
            if (value != null && !value.isEmpty()) {
                hashData.append(field).append('=').append(value).append('&');
                query.append(URLEncoder.encode(field, StandardCharsets.UTF_8))
                     .append('=')
                     .append(URLEncoder.encode(value, StandardCharsets.UTF_8))
                     .append('&');
            }
        }

        hashData.setLength(hashData.length() - 1);
        query.setLength(query.length() - 1);

        String secureHash = hmacSHA512(hashSecret, hashData.toString());
        query.append("&vnp_SecureHash=").append(secureHash);

        return payUrl + "?" + query;
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "HmacSHA512");
            mac.init(secretKey);
            byte[] raw = mac.doFinal(data.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : raw) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error creating hash", e);
        }
    }
}
