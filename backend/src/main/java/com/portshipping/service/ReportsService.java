package com.portshipping.service;

import com.portshipping.dto.*;
import com.portshipping.entity.Invoice;
import com.portshipping.entity.Payment;
import com.portshipping.entity.Shipment;
import com.portshipping.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportsService {

    private final ShipmentRepository shipmentRepository;
    private final VesselRepository vesselRepository;
    private final ContainerRepository containerRepository;
    private final CustomsClearanceRepository customsClearanceRepository;
    private final BerthAllocationRepository berthAllocationRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final ReportsPdfService reportsPdfService;

    public ReportsService(ShipmentRepository shipmentRepository,
                          VesselRepository vesselRepository,
                          ContainerRepository containerRepository,
                          CustomsClearanceRepository customsClearanceRepository,
                          BerthAllocationRepository berthAllocationRepository,
                          InvoiceRepository invoiceRepository,
                          PaymentRepository paymentRepository,
                          ReportsPdfService reportsPdfService) {
        this.shipmentRepository = shipmentRepository;
        this.vesselRepository = vesselRepository;
        this.containerRepository = containerRepository;
        this.customsClearanceRepository = customsClearanceRepository;
        this.berthAllocationRepository = berthAllocationRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.reportsPdfService = reportsPdfService;
    }

    public OperationalSummaryDTO getOperationalSummary(String startDateStr, String endDateStr) {
        DateRange range = parseAndValidateDateRange(startDateStr, endDateStr);

        List<Shipment> shipments = shipmentRepository.findAll();
        if (range.hasRange()) {
            shipments = shipments.stream()
                    .filter(s -> s.getCreatedAt() != null &&
                            (range.getStartInstant() == null || !s.getCreatedAt().isBefore(range.getStartInstant())) &&
                            (range.getEndInstant() == null || !s.getCreatedAt().isAfter(range.getEndInstant())))
                    .collect(Collectors.toList());
        }

        long totalShipments = shipments.size();
        long activeShipments = shipments.stream()
                .filter(s -> "IN_TRANSIT".equalsIgnoreCase(s.getStatus()) || "BOOKED".equalsIgnoreCase(s.getStatus()))
                .count();
        long deliveredShipments = shipments.stream()
                .filter(s -> "DELIVERED".equalsIgnoreCase(s.getStatus()))
                .count();

        long totalVessels = vesselRepository.count();
        long vesselsInPort = vesselRepository.findAll().stream()
                .filter(v -> "IN_PORT".equalsIgnoreCase(v.getStatus()) || "DOCKED".equalsIgnoreCase(v.getStatus()))
                .count();

        long totalContainers = containerRepository.count();
        long containersInHold = containerRepository.findAll().stream()
                .filter(c -> "CUSTOMS_HOLD".equalsIgnoreCase(c.getStatus()) || "HOLD".equalsIgnoreCase(c.getStatus()))
                .count();

        long pendingCustoms = customsClearanceRepository.findAll().stream()
                .filter(c -> "PENDING".equalsIgnoreCase(c.getStatus()) || "UNDER_INSPECTION".equalsIgnoreCase(c.getStatus()))
                .count();
        long approvedCustoms = customsClearanceRepository.findAll().stream()
                .filter(c -> "APPROVED".equalsIgnoreCase(c.getStatus()) || "CLEARED".equalsIgnoreCase(c.getStatus()))
                .count();

        long allocatedBerths = berthAllocationRepository.findAll().stream()
                .filter(b -> "SCHEDULED".equalsIgnoreCase(b.getStatus()) || "OCCUPIED".equalsIgnoreCase(b.getStatus()))
                .count();

        return new OperationalSummaryDTO(
                totalVessels, vesselsInPort, totalContainers, containersInHold,
                totalShipments, activeShipments, deliveredShipments,
                pendingCustoms, approvedCustoms, allocatedBerths
        );
    }

    public List<StatusDistributionDTO> getShipmentStatusDistribution(String startDateStr, String endDateStr) {
        DateRange range = parseAndValidateDateRange(startDateStr, endDateStr);

        List<Shipment> shipments = shipmentRepository.findAll();
        if (range.hasRange()) {
            shipments = shipments.stream()
                    .filter(s -> s.getCreatedAt() != null &&
                            (range.getStartInstant() == null || !s.getCreatedAt().isBefore(range.getStartInstant())) &&
                            (range.getEndInstant() == null || !s.getCreatedAt().isAfter(range.getEndInstant())))
                    .collect(Collectors.toList());
        }

        Map<String, Long> map = shipments.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getStatus() != null ? s.getStatus() : "UNKNOWN",
                        Collectors.counting()
                ));

        return map.entrySet().stream()
                .map(e -> new StatusDistributionDTO(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(StatusDistributionDTO::getStatus))
                .collect(Collectors.toList());
    }

    public List<StatusDistributionDTO> getVesselActivitySummary() {
        Map<String, Long> map = vesselRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        v -> v.getStatus() != null ? v.getStatus() : "UNKNOWN",
                        Collectors.counting()
                ));

        return map.entrySet().stream()
                .map(e -> new StatusDistributionDTO(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(StatusDistributionDTO::getStatus))
                .collect(Collectors.toList());
    }

    public List<StatusDistributionDTO> getContainerSummary() {
        Map<String, Long> map = containerRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        c -> c.getStatus() != null ? c.getStatus() : "UNKNOWN",
                        Collectors.counting()
                ));

        return map.entrySet().stream()
                .map(e -> new StatusDistributionDTO(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(StatusDistributionDTO::getStatus))
                .collect(Collectors.toList());
    }

    public List<StatusDistributionDTO> getCustomsSummary() {
        Map<String, Long> map = customsClearanceRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        c -> c.getStatus() != null ? c.getStatus() : "UNKNOWN",
                        Collectors.counting()
                ));

        return map.entrySet().stream()
                .map(e -> new StatusDistributionDTO(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(StatusDistributionDTO::getStatus))
                .collect(Collectors.toList());
    }

    public FinancialAnalyticsDTO getFinancialSummary(String startDateStr, String endDateStr) {
        DateRange range = parseAndValidateDateRange(startDateStr, endDateStr);

        List<Invoice> invoices = invoiceRepository.findAll();
        if (range.hasRange()) {
            invoices = invoices.stream()
                    .filter(i -> i.getCreatedAt() != null &&
                            (range.getStartInstant() == null || !i.getCreatedAt().isBefore(range.getStartInstant())) &&
                            (range.getEndInstant() == null || !i.getCreatedAt().isAfter(range.getEndInstant())))
                    .collect(Collectors.toList());
        }

        BigDecimal totalInvoiced = BigDecimal.ZERO;
        BigDecimal overdueAmount = BigDecimal.ZERO;
        long overdueCount = 0;
        Map<String, Long> statusCounts = new HashMap<>();

        for (Invoice inv : invoices) {
            String status = inv.getPaymentStatus() != null ? inv.getPaymentStatus() : "PENDING";
            statusCounts.put(status, statusCounts.getOrDefault(status, 0L) + 1);

            if (!"CANCELLED".equalsIgnoreCase(status)) {
                BigDecimal charges = inv.getCharges() != null ? inv.getCharges() : BigDecimal.ZERO;
                BigDecimal taxes = inv.getTaxes() != null ? inv.getTaxes() : BigDecimal.ZERO;
                BigDecimal discount = inv.getDiscount() != null ? inv.getDiscount() : BigDecimal.ZERO;
                BigDecimal invTotal = charges.add(taxes).subtract(discount).setScale(2, RoundingMode.HALF_UP);

                totalInvoiced = totalInvoiced.add(invTotal);

                BigDecimal paid = paymentRepository.sumAmountByInvoiceId(inv.getId());
                if (paid == null) paid = BigDecimal.ZERO;
                BigDecimal rem = invTotal.subtract(paid);

                if ("OVERDUE".equalsIgnoreCase(status) || (inv.getDueDate() != null && inv.getDueDate().isBefore(LocalDate.now()) && rem.compareTo(BigDecimal.ZERO) > 0)) {
                    overdueCount++;
                    if (rem.compareTo(BigDecimal.ZERO) > 0) {
                        overdueAmount = overdueAmount.add(rem);
                    }
                }
            }
        }

        List<Payment> payments = paymentRepository.findAll();
        if (range.hasRange()) {
            payments = payments.stream()
                    .filter(p -> p.getPaidAt() != null &&
                            (range.getStart() == null || !p.getPaidAt().isBefore(range.getStart())) &&
                            (range.getEnd() == null || !p.getPaidAt().isAfter(range.getEnd())))
                    .collect(Collectors.toList());
        }

        BigDecimal totalCollected = payments.stream()
                .map(Payment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal outstandingBalance = totalInvoiced.subtract(totalCollected);
        if (outstandingBalance.compareTo(BigDecimal.ZERO) < 0) {
            outstandingBalance = BigDecimal.ZERO;
        }

        return new FinancialAnalyticsDTO(
                totalInvoiced.setScale(2, RoundingMode.HALF_UP),
                totalCollected.setScale(2, RoundingMode.HALF_UP),
                outstandingBalance.setScale(2, RoundingMode.HALF_UP),
                overdueAmount.setScale(2, RoundingMode.HALF_UP),
                invoices.size(),
                overdueCount,
                statusCounts
        );
    }

    public List<TimeSeriesPointDTO> getRevenueTrends(String startDateStr, String endDateStr, String interval) {
        DateRange range = parseAndValidateDateRange(startDateStr, endDateStr);
        List<Payment> payments = paymentRepository.findAll();

        if (range.hasRange()) {
            payments = payments.stream()
                    .filter(p -> p.getPaidAt() != null &&
                            (range.getStart() == null || !p.getPaidAt().isBefore(range.getStart())) &&
                            (range.getEnd() == null || !p.getPaidAt().isAfter(range.getEnd())))
                    .collect(Collectors.toList());
        }

        Map<String, List<Payment>> grouped = new TreeMap<>();
        DateTimeFormatter fmt = "MONTHLY".equalsIgnoreCase(interval) ?
                DateTimeFormatter.ofPattern("yyyy-MM") :
                DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Payment p : payments) {
            if (p.getPaidAt() != null) {
                String key = fmt.format(p.getPaidAt());
                grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
            }
        }

        List<TimeSeriesPointDTO> result = new ArrayList<>();
        for (Map.Entry<String, List<Payment>> entry : grouped.entrySet()) {
            BigDecimal sum = entry.getValue().stream()
                    .map(Payment::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            result.add(new TimeSeriesPointDTO(entry.getKey(), sum, entry.getValue().size()));
        }

        if (result.isEmpty()) {
            result.add(new TimeSeriesPointDTO(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), BigDecimal.ZERO, 0));
        }

        return result;
    }

    public byte[] exportCsvReport(String reportType, String startDateStr, String endDateStr) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out);

        if ("financial".equalsIgnoreCase(reportType)) {
            FinancialAnalyticsDTO fin = getFinancialSummary(startDateStr, endDateStr);
            writer.println("Metric,Value");
            writer.println(sanitizeCsvCell("Total Invoiced") + "," + sanitizeCsvCell("$" + fin.getTotalInvoiced()));
            writer.println(sanitizeCsvCell("Total Collected") + "," + sanitizeCsvCell("$" + fin.getTotalCollected()));
            writer.println(sanitizeCsvCell("Outstanding Balance") + "," + sanitizeCsvCell("$" + fin.getOutstandingBalance()));
            writer.println(sanitizeCsvCell("Overdue Amount") + "," + sanitizeCsvCell("$" + fin.getOverdueAmount()));
            writer.println(sanitizeCsvCell("Total Invoices Count") + "," + fin.getTotalInvoiceCount());
            writer.println(sanitizeCsvCell("Overdue Invoices Count") + "," + fin.getOverdueCount());
        } else if ("vessels".equalsIgnoreCase(reportType)) {
            List<StatusDistributionDTO> list = getVesselActivitySummary();
            writer.println("Vessel Status,Count");
            for (StatusDistributionDTO item : list) {
                writer.println(sanitizeCsvCell(item.getStatus()) + "," + item.getCount());
            }
        } else if ("containers".equalsIgnoreCase(reportType)) {
            List<StatusDistributionDTO> list = getContainerSummary();
            writer.println("Container Status,Count");
            for (StatusDistributionDTO item : list) {
                writer.println(sanitizeCsvCell(item.getStatus()) + "," + item.getCount());
            }
        } else if ("customs".equalsIgnoreCase(reportType)) {
            List<StatusDistributionDTO> list = getCustomsSummary();
            writer.println("Customs Status,Count");
            for (StatusDistributionDTO item : list) {
                writer.println(sanitizeCsvCell(item.getStatus()) + "," + item.getCount());
            }
        } else {
            // Default: shipments
            List<StatusDistributionDTO> list = getShipmentStatusDistribution(startDateStr, endDateStr);
            writer.println("Shipment Status,Count");
            for (StatusDistributionDTO item : list) {
                writer.println(sanitizeCsvCell(item.getStatus()) + "," + item.getCount());
            }
        }

        writer.flush();
        return out.toByteArray();
    }

    public byte[] exportPdfReport(String reportType, String startDateStr, String endDateStr, boolean includeFinancial) throws Exception {
        OperationalSummaryDTO op = getOperationalSummary(startDateStr, endDateStr);
        List<StatusDistributionDTO> shipmentStatuses = getShipmentStatusDistribution(startDateStr, endDateStr);
        FinancialAnalyticsDTO fin = includeFinancial ? getFinancialSummary(startDateStr, endDateStr) : null;

        String title = reportType != null ? reportType.toUpperCase() : "GENERAL SUMMARY";
        return reportsPdfService.generateSummaryPdf(title, startDateStr, endDateStr, op, fin, shipmentStatuses);
    }

    public String sanitizeCsvCell(String value) {
        if (value == null) return "\"\"";
        String str = value.trim();

        // Prevent formula injection
        if (str.startsWith("=") || str.startsWith("+") || str.startsWith("-") || str.startsWith("@") || str.startsWith("\t") || str.startsWith("\r")) {
            str = "'" + str;
        }

        // Escape double quotes by doubling them
        str = str.replace("\"", "\"\"");
        return "\"" + str + "\"";
    }

    private DateRange parseAndValidateDateRange(String startDateStr, String endDateStr) {
        LocalDateTime start = null;
        LocalDateTime end = null;

        if (startDateStr != null && !startDateStr.isBlank()) {
            try {
                LocalDate sd = LocalDate.parse(startDateStr.trim());
                start = sd.atStartOfDay();
            } catch (DateTimeParseException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid startDate format. Expected YYYY-MM-DD.");
            }
        }

        if (endDateStr != null && !endDateStr.isBlank()) {
            try {
                LocalDate ed = LocalDate.parse(endDateStr.trim());
                end = ed.atTime(LocalTime.MAX);
            } catch (DateTimeParseException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid endDate format. Expected YYYY-MM-DD.");
            }
        }

        if (start != null && end != null && start.isAfter(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate cannot be after endDate.");
        }

        return new DateRange(start, end);
    }

    private static class DateRange {
        private final LocalDateTime start;
        private final LocalDateTime end;
        private final Instant startInstant;
        private final Instant endInstant;

        public DateRange(LocalDateTime start, LocalDateTime end) {
            this.start = start;
            this.end = end;
            this.startInstant = start != null ? start.toInstant(ZoneOffset.UTC) : null;
            this.endInstant = end != null ? end.toInstant(ZoneOffset.UTC) : null;
        }

        public boolean hasRange() {
            return start != null || end != null;
        }

        public LocalDateTime getStart() { return start; }
        public LocalDateTime getEnd() { return end; }
        public Instant getStartInstant() { return startInstant; }
        public Instant getEndInstant() { return endInstant; }
    }
}
