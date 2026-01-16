package com.nguyenthithuhuyen.example10.payment;

import com.nguyenthithuhuyen.example10.entity.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

public String createPaymentUrl(Order order, String ipAddr) {

    Map<String, String> params = new HashMap<>();

    params.put("vnp_Version", "2.1.0");
    params.put("vnp_Command", "pay");
    params.put("vnp_TmnCode", tmnCode.trim());
    params.put("vnp_Amount",
            order.getFinalAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .toBigInteger()
                    .toString());
    params.put("vnp_CurrCode", "VND");
    params.put("vnp_TxnRef", String.valueOf(order.getId()));
    params.put("vnp_OrderInfo", "Thanh toan don hang " + order.getId());
    params.put("vnp_OrderType", "other");
    params.put("vnp_Locale", "vn");
    params.put("vnp_IpAddr", ipAddr);
    params.put("vnp_ReturnUrl", returnUrl.trim());
    params.put("vnp_CreateDate",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

    // ⚠️ BẮT BUỘC CÓ
    params.put("vnp_SecureHashType", "HmacSHA512");

    // ===== SORT =====
    List<String> keys = new ArrayList<>(params.keySet());
    Collections.sort(keys);

    StringBuilder hashData = new StringBuilder();
    StringBuilder query = new StringBuilder();

    for (String key : keys) {
        String value = params.get(key);
        if (value != null && !value.isEmpty()) {

            String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8);

            hashData.append(key)
                    .append("=")
                    .append(encodedValue)
                    .append("&");

            query.append(key)
                    .append("=")
                    .append(encodedValue)
                    .append("&");
        }
    }

    hashData.setLength(hashData.length() - 1);
    query.setLength(query.length() - 1);

    String secureHash = hmacSHA512(hashSecret.trim(), hashData.toString());

    return payUrl
            + "?"
            + query
            + "&vnp_SecureHash="
            + secureHash;
}
    public boolean verifyCallback(Map<String, String> params) {

    String receivedHash = params.get("vnp_SecureHash");

    // REMOVE HASH PARAMS
    Map<String, String> filtered = new TreeMap<>();
    for (Map.Entry<String, String> entry : params.entrySet()) {
        if (!entry.getKey().equals("vnp_SecureHash")
                && !entry.getKey().equals("vnp_SecureHashType")
                && entry.getValue() != null
                && !entry.getValue().isEmpty()) {
            filtered.put(entry.getKey(), entry.getValue());
        }
    }

    // BUILD HASH DATA
    StringBuilder hashData = new StringBuilder();
    for (Map.Entry<String, String> entry : filtered.entrySet()) {
        hashData.append(entry.getKey())
                .append("=")
                .append(entry.getValue())
                .append("&");
    }
    hashData.setLength(hashData.length() - 1);

    String calculatedHash = hmacSHA512(hashSecret, hashData.toString());

    return calculatedHash.equalsIgnoreCase(receivedHash);
}

    private String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : raw) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
