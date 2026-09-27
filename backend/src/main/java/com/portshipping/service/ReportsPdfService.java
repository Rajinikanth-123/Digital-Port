package com.portshipping.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.portshipping.dto.FinancialAnalyticsDTO;
import com.portshipping.dto.OperationalSummaryDTO;
import com.portshipping.dto.StatusDistributionDTO;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportsPdfService {

    public byte[] generateSummaryPdf(String reportTitle,
                                     String startDate,
                                     String endDate,
                                     OperationalSummaryDTO opSummary,
                                     FinancialAnalyticsDTO finSummary,
                                     List<StatusDistributionDTO> shipmentStatuses) throws Exception {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Color.DARK_GRAY);
        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(40, 80, 150));
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);

        // Header Title
        Paragraph header = new Paragraph("HARBOR PORT & SHIPPING SYSTEM", titleFont);
        document.add(header);

        Paragraph subHeader = new Paragraph("Official Analytics & Operations Report: " + reportTitle, subtitleFont);
        subHeader.setSpacingAfter(10);
        document.add(subHeader);

        // Filter / Time Context
        Paragraph filterInfo = new Paragraph(String.format("Generated On: %s  |  Filter Window: %s to %s",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                (startDate != null ? startDate : "Beginning"),
                (endDate != null ? endDate : "Present")), subtitleFont);
        filterInfo.setSpacingAfter(20);
        document.add(filterInfo);

        // Operational Overview Section
        if (opSummary != null) {
            document.add(new Paragraph("1. Operational Summary Metrics", sectionFont));
            document.add(Chunk.NEWLINE);

            PdfPTable opTable = new PdfPTable(2);
            opTable.setWidthPercentage(100);
            opTable.setWidths(new float[]{3f, 2f});

            addTableCell(opTable, "Metric Category", headerFont, new Color(230, 235, 245));
            addTableCell(opTable, "Count / Volume", headerFont, new Color(230, 235, 245));

            addTableCell(opTable, "Total Fleet Vessels", normalFont, null);
            addTableCell(opTable, String.valueOf(opSummary.getTotalVessels()), normalFont, null);

            addTableCell(opTable, "Vessels Currently In Port", normalFont, null);
            addTableCell(opTable, String.valueOf(opSummary.getVesselsInPort()), normalFont, null);

            addTableCell(opTable, "Total Inventory Containers", normalFont, null);
            addTableCell(opTable, String.valueOf(opSummary.getTotalContainers()), normalFont, null);

            addTableCell(opTable, "Containers on Customs Hold", normalFont, null);
            addTableCell(opTable, String.valueOf(opSummary.getContainersInHold()), normalFont, null);

            addTableCell(opTable, "Total Recorded Shipments", normalFont, null);
            addTableCell(opTable, String.valueOf(opSummary.getTotalShipments()), normalFont, null);

            addTableCell(opTable, "Active Shipments In Transit", normalFont, null);
            addTableCell(opTable, String.valueOf(opSummary.getActiveShipments()), normalFont, null);

            addTableCell(opTable, "Pending Customs Declarations", normalFont, null);
            addTableCell(opTable, String.valueOf(opSummary.getPendingCustoms()), normalFont, null);

            opTable.setSpacingAfter(20);
            document.add(opTable);
        }

        // Shipment Status Distribution Section
        if (shipmentStatuses != null && !shipmentStatuses.isEmpty()) {
            document.add(new Paragraph("2. Shipment Status Breakdown", sectionFont));
            document.add(Chunk.NEWLINE);

            PdfPTable statusTable = new PdfPTable(2);
            statusTable.setWidthPercentage(100);
            statusTable.setWidths(new float[]{3f, 2f});

            addTableCell(statusTable, "Shipment Status", headerFont, new Color(230, 235, 245));
            addTableCell(statusTable, "Shipments Count", headerFont, new Color(230, 235, 245));

            for (StatusDistributionDTO s : shipmentStatuses) {
                addTableCell(statusTable, s.getStatus(), normalFont, null);
                addTableCell(statusTable, String.valueOf(s.getCount()), normalFont, null);
            }

            statusTable.setSpacingAfter(20);
            document.add(statusTable);
        }

        // Financial Section (If authorized and provided)
        if (finSummary != null) {
            document.add(new Paragraph("3. Financial & Revenue Performance Summary", sectionFont));
            document.add(Chunk.NEWLINE);

            PdfPTable finTable = new PdfPTable(2);
            finTable.setWidthPercentage(100);
            finTable.setWidths(new float[]{3f, 2f});

            addTableCell(finTable, "Financial Indicator", headerFont, new Color(230, 245, 235));
            addTableCell(finTable, "Amount (USD $)", headerFont, new Color(230, 245, 235));

            addTableCell(finTable, "Gross Amount Invoiced", normalFont, null);
            addTableCell(finTable, String.format("$%.2f", finSummary.getTotalInvoiced()), normalFont, null);

            addTableCell(finTable, "Total Collections Received", normalFont, null);
            addTableCell(finTable, String.format("$%.2f", finSummary.getTotalCollected()), normalFont, null);

            addTableCell(finTable, "Outstanding Receivables Balance", normalFont, null);
            addTableCell(finTable, String.format("$%.2f", finSummary.getOutstandingBalance()), normalFont, null);

            addTableCell(finTable, "Overdue Accounts Amount", normalFont, null);
            addTableCell(finTable, String.format("$%.2f (%d invoices)", finSummary.getOverdueAmount(), finSummary.getOverdueCount()), normalFont, null);

            finTable.setSpacingAfter(20);
            document.add(finTable);
        }

        Paragraph footer = new Paragraph("HarborOS Digital Port & Shipping Operations — Confidential Executive Summary", subtitleFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        document.close();
        return out.toByteArray();
    }

    private void addTableCell(PdfPTable table, String text, Font font, Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setPadding(6);
        if (bgColor != null) {
            cell.setBackgroundColor(bgColor);
        }
        table.addCell(cell);
    }
}
