package com.nguyenthithuhuyen.example10.security.services;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;





@Service
public class QrCodeService {

    public byte[] generateQrCode(String text) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(
                    text,
                    BarcodeFormat.QR_CODE,
                    300,
                    300
            );

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(
                    bitMatrix,
                    "PNG",
                    pngOutputStream
            );
            return pngOutputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR code");
        }
    }
}
