package com.nguyenthithuhuyen.example10.security.services;

import com.nguyenthithuhuyen.example10.entity.*;
import com.nguyenthithuhuyen.example10.entity.enums.OrderStatus;
import com.nguyenthithuhuyen.example10.entity.enums.PaymentStatus;
import com.nguyenthithuhuyen.example10.payload.request.BillRequest;
import com.nguyenthithuhuyen.example10.repository.BillRepository;
import com.nguyenthithuhuyen.example10.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.List;
import com.itextpdf.text.Element;

@Service
@RequiredArgsConstructor
public class BillService {

    private final BillRepository billRepository;
    private final OrderRepository orderRepository;

    /* ==========================================================
       TẠO BILL (SAU KHI THANH TOÁN)
       ========================================================== */
    @Transactional
    public Bill create(BillRequest request) {

        if (request.getOrderId() == null) {
            throw new IllegalArgumentException("Order ID không được để trống");
        }

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy đơn hàng " + request.getOrderId()));

        // ❌ Không cho tạo trùng bill
        if (billRepository.existsByOrderId(order.getId())) {
            throw new RuntimeException("Hóa đơn đã tồn tại cho đơn hàng " + order.getId());
        }

        // ✅ Khi tạo bill = đã thanh toán
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        Bill bill = Bill.builder()
                .order(order)
                .totalAmount(order.getFinalAmount())
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.COMPLETED)
                .note(request.getNote())
                .issuedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return billRepository.save(bill);
    }

    public Bill getById(Long id) {
        return billRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy hóa đơn #" + id));
    }

    public List<Bill> getAll() {
        return billRepository.findAll();
    }

    @Transactional
    public Bill update(Long id, Bill billUpdateData) {

        Bill existing = billRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy hóa đơn #" + id));

        PaymentStatus oldStatus = existing.getPaymentStatus();

        if (billUpdateData.getPaymentStatus() != null)
            existing.setPaymentStatus(billUpdateData.getPaymentStatus());

        if (billUpdateData.getPaymentMethod() != null)
            existing.setPaymentMethod(billUpdateData.getPaymentMethod());

        if (billUpdateData.getNote() != null)
            existing.setNote(billUpdateData.getNote());

        if (billUpdateData.getTotalAmount() != null)
            existing.setTotalAmount(billUpdateData.getTotalAmount());

        existing.setUpdatedAt(LocalDateTime.now());

        Bill saved = billRepository.save(existing);

        // Nếu chuyển sang COMPLETED → đảm bảo Order = PAID
        if (oldStatus != PaymentStatus.COMPLETED
                && saved.getPaymentStatus() == PaymentStatus.COMPLETED) {

            Order order = saved.getOrder();
            if (order != null) {
                order.setStatus(OrderStatus.PAID);
                orderRepository.save(order);
            }
        }

        return saved;
    }

    public void delete(Long id) {
        billRepository.deleteById(id);
    }

    /* ==========================================================
       EXPORT PDF
       ========================================================== */
    public byte[] exportToPdfBytes(Long billId) {
        Bill bill = getById(billId);
        checkCanExport(bill);
        return generatePdfBytes(bill);
    }

    private void checkCanExport(Bill bill) {
        if (bill.getPaymentStatus() != PaymentStatus.COMPLETED) {
            throw new RuntimeException(
                    "Chỉ xuất PDF khi hóa đơn ở trạng thái COMPLETED");
        }
    }

    /* ==========================================================
       PDF CORE
       ========================================================== */
    private byte[] generatePdfBytes(Bill bill) {

        Document document = new Document();

        try {
            Font font = getVietnameseFont();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            addBillContent(document, bill, font);

            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo PDF", e);
        }
    }

    private void addBillContent(Document document, Bill bill, Font font)
            throws DocumentException {

        DecimalFormat df = new DecimalFormat("#,##0.00");

        Font titleFont = new Font(font.getBaseFont(), 18, Font.BOLD);
        Font boldFont = new Font(font.getBaseFont(), 12, Font.BOLD);

        Paragraph title = new Paragraph("🧾 HÓA ĐƠN THANH TOÁN", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(new Paragraph("\n"));

        document.add(new Paragraph("Mã hóa đơn: #" + bill.getId(), font));
        document.add(new Paragraph("Ngày xuất: " + bill.getIssuedAt(), font));

        if (bill.getOrder() != null && bill.getOrder().getUser() != null) {
            document.add(new Paragraph(
                    "Khách hàng: " + bill.getOrder().getUser().getFullName(), font));
        }

        document.add(new Paragraph("\n"));
        document.add(new Paragraph("📦 Danh sách sản phẩm", boldFont));

        PdfPTable table = new PdfPTable(new float[]{4, 1, 2, 2});
        table.setWidthPercentage(100);

        String[] headers = {"Sản phẩm", "SL", "Đơn giá", "Thành tiền"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, boldFont));
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        for (OrderItem item : bill.getOrder().getOrderItems()) {
            table.addCell(new Phrase(item.getProduct().getName(), font));
            table.addCell(new Phrase(String.valueOf(item.getQuantity()), font));
            table.addCell(new Phrase(df.format(item.getPrice()), font));
            table.addCell(new Phrase(df.format(item.getSubtotal()), font));
        }

        document.add(table);
        document.add(new Paragraph("\n"));

        document.add(new Paragraph("Tổng tiền: "
                + df.format(bill.getTotalAmount()) + " VND", boldFont));
        document.add(new Paragraph("Thanh toán: "
                + bill.getPaymentMethod(), font));
        document.add(new Paragraph("Trạng thái: "
                + bill.getPaymentStatus(), font));

        document.add(new Paragraph("\n"));
        document.add(new Paragraph("Cảm ơn quý khách!", titleFont));
    }

    /* ==========================================================
       FONT TIẾNG VIỆT
       ========================================================== */
    private Font getVietnameseFont() throws Exception {
        BaseFont bf = BaseFont.createFont(
                "C:\\Windows\\Fonts\\arial.ttf",
                BaseFont.IDENTITY_H,
                BaseFont.EMBEDDED
        );
        return new Font(bf, 12);
    }
}
