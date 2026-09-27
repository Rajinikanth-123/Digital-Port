package com.portshipping.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.portshipping.entity.Invoice;
import com.portshipping.entity.InvoiceItem;
import com.portshipping.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

@Service
public class InvoicePdfService {

    private final PaymentRepository paymentRepository;

    public InvoicePdfService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public byte[] generateInvoicePdf(Invoice invoice) throws Exception {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();

        // Title Header
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Color.DARK_GRAY);
        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);

        Paragraph header = new Paragraph("HARBOR PORT & SHIPPING SYSTEM", titleFont);
        header.setAlignment(Element.ALIGN_LEFT);
        document.add(header);

        Paragraph subHeader = new Paragraph("Official Commercial Invoice & Port Dues Statement", subtitleFont);
        subHeader.setAlignment(Element.ALIGN_LEFT);
        subHeader.setSpacingAfter(15);
        document.add(subHeader);

        // Metadata Table
        PdfPTable metaTable = new PdfPTable(2);
        metaTable.setWidthPercentage(100);
        metaTable.setWidths(new float[]{1f, 1f});

        PdfPCell cellLeft = new PdfPCell();
        cellLeft.setBorder(Rectangle.NO_BORDER);
        cellLeft.addElement(new Paragraph("CUSTOMER DETAILS", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLUE)));
        cellLeft.addElement(new Paragraph("Customer: " + invoice.getCustomerName(), normalFont));
        if (invoice.getShipment() != null) {
            cellLeft.addElement(new Paragraph("Linked Shipment: " + invoice.getShipment().getTrackingNumber(), normalFont));
            cellLeft.addElement(new Paragraph("Route: " + invoice.getShipment().getOrigin() + " → " + invoice.getShipment().getDestination(), normalFont));
        }

        PdfPCell cellRight = new PdfPCell();
        cellRight.setBorder(Rectangle.NO_BORDER);
        cellRight.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cellRight.addElement(new Paragraph("INVOICE SUMMARY", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLUE)));
        cellRight.addElement(new Paragraph("Invoice Number: " + invoice.getInvoiceNumber(), boldFont));
        cellRight.addElement(new Paragraph("Due Date: " + (invoice.getDueDate() != null ? invoice.getDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : "N/A"), normalFont));
        cellRight.addElement(new Paragraph("Currency: " + invoice.getCurrency(), normalFont));
        cellRight.addElement(new Paragraph("Payment Status: " + invoice.getPaymentStatus(), boldFont));

        metaTable.addCell(cellLeft);
        metaTable.addCell(cellRight);
        metaTable.setSpacingAfter(20);
        document.add(metaTable);

        // Line Items Table
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3f, 1f, 1.5f, 1.5f});

        addTableHeader(table, "Item Description", boldFont);
        addTableHeader(table, "Qty", boldFont);
        addTableHeader(table, "Unit Price (" + invoice.getCurrency() + ")", boldFont);
        addTableHeader(table, "Line Total (" + invoice.getCurrency() + ")", boldFont);

        BigDecimal itemsSubtotal = BigDecimal.ZERO;
        if (invoice.getItems() != null && !invoice.getItems().isEmpty()) {
            for (InvoiceItem item : invoice.getItems()) {
                BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ONE;
                BigDecimal price = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                BigDecimal lineTot = qty.multiply(price).setScale(2, RoundingMode.HALF_UP);
                itemsSubtotal = itemsSubtotal.add(lineTot);

                table.addCell(new Phrase(item.getDescription(), normalFont));
                table.addCell(new Phrase(qty.toString(), normalFont));
                table.addCell(new Phrase(String.format("%.2f", price), normalFont));
                table.addCell(new Phrase(String.format("%.2f", lineTot), normalFont));
            }
        } else {
            table.addCell(new Phrase("Port Operations & Shipping Freight Service Charges", normalFont));
            table.addCell(new Phrase("1", normalFont));
            table.addCell(new Phrase(String.format("%.2f", invoice.getCharges()), normalFont));
            table.addCell(new Phrase(String.format("%.2f", invoice.getCharges()), normalFont));
            itemsSubtotal = invoice.getCharges();
        }

        table.setSpacingAfter(20);
        document.add(table);

        // Totals Calculation Table
        BigDecimal charges = invoice.getCharges() != null ? invoice.getCharges() : itemsSubtotal;
        BigDecimal taxes = invoice.getTaxes() != null ? invoice.getTaxes() : BigDecimal.ZERO;
        BigDecimal discount = invoice.getDiscount() != null ? invoice.getDiscount() : BigDecimal.ZERO;
        BigDecimal storedTotal = charges.add(taxes).subtract(discount).setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalPaid = paymentRepository.sumAmountByInvoiceId(invoice.getId());
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;

        BigDecimal balance = storedTotal.subtract(totalPaid);
        if (balance.compareTo(BigDecimal.ZERO) < 0) balance = BigDecimal.ZERO;

        PdfPTable totalsTable = new PdfPTable(2);
        totalsTable.setWidthPercentage(40);
        totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

        addTotalsRow(totalsTable, "Subtotal / Charges:", String.format("%.2f %s", charges, invoice.getCurrency()), normalFont);
        addTotalsRow(totalsTable, "Taxes:", String.format("%.2f %s", taxes, invoice.getCurrency()), normalFont);
        addTotalsRow(totalsTable, "Discount:", String.format("-%.2f %s", discount, invoice.getCurrency()), normalFont);
        addTotalsRow(totalsTable, "Total Amount Billed:", String.format("%.2f %s", storedTotal, invoice.getCurrency()), boldFont);
        addTotalsRow(totalsTable, "Total Paid to Date:", String.format("%.2f %s", totalPaid, invoice.getCurrency()), normalFont);
        addTotalsRow(totalsTable, "Outstanding Balance:", String.format("%.2f %s", balance, invoice.getCurrency()), boldFont);

        document.add(totalsTable);

        // Footer Note
        Paragraph footer = new Paragraph("\nThank you for choosing Harbor Port Services. Please remit payments promptly by the due date.", subtitleFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        document.close();
        return out.toByteArray();
    }

    private void addTableHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(230, 235, 245));
        cell.setPadding(6);
        table.addCell(cell);
    }

    private void addTotalsRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, font));
        c1.setBorder(Rectangle.NO_BORDER);
        PdfPCell c2 = new PdfPCell(new Phrase(value, font));
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(c1);
        table.addCell(c2);
    }
}
