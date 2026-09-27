package com.portshipping.controller;

import com.portshipping.dto.*;
import com.portshipping.service.ReportsService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportsController {

    private final ReportsService reportsService;

    public ReportsController(ReportsService reportsService) {
        this.reportsService = reportsService;
    }

    @GetMapping("/operational-summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'PORT_MANAGER', 'SHIPPING_AGENT', 'VIEWER', 'FINANCE_OFFICER')")
    public ResponseEntity<OperationalSummaryDTO> getOperationalSummary(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ResponseEntity.ok(reportsService.getOperationalSummary(startDate, endDate));
    }

    @GetMapping("/shipment-distribution")
    @PreAuthorize("hasAnyRole('ADMIN', 'PORT_MANAGER', 'SHIPPING_AGENT', 'VIEWER', 'FINANCE_OFFICER')")
    public ResponseEntity<List<StatusDistributionDTO>> getShipmentStatusDistribution(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ResponseEntity.ok(reportsService.getShipmentStatusDistribution(startDate, endDate));
    }

    @GetMapping("/vessel-activity")
    @PreAuthorize("hasAnyRole('ADMIN', 'PORT_MANAGER', 'SHIPPING_AGENT', 'VIEWER', 'FINANCE_OFFICER')")
    public ResponseEntity<List<StatusDistributionDTO>> getVesselActivity() {
        return ResponseEntity.ok(reportsService.getVesselActivitySummary());
    }

    @GetMapping("/container-cargo-summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'PORT_MANAGER', 'SHIPPING_AGENT', 'VIEWER', 'FINANCE_OFFICER')")
    public ResponseEntity<List<StatusDistributionDTO>> getContainerSummary() {
        return ResponseEntity.ok(reportsService.getContainerSummary());
    }

    @GetMapping("/customs-summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'PORT_MANAGER', 'SHIPPING_AGENT', 'VIEWER', 'FINANCE_OFFICER')")
    public ResponseEntity<List<StatusDistributionDTO>> getCustomsSummary() {
        return ResponseEntity.ok(reportsService.getCustomsSummary());
    }

    @GetMapping("/financial-summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'PORT_MANAGER', 'FINANCE_OFFICER')")
    public ResponseEntity<FinancialAnalyticsDTO> getFinancialSummary(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ResponseEntity.ok(reportsService.getFinancialSummary(startDate, endDate));
    }

    @GetMapping("/revenue-trends")
    @PreAuthorize("hasAnyRole('ADMIN', 'PORT_MANAGER', 'FINANCE_OFFICER')")
    public ResponseEntity<List<TimeSeriesPointDTO>> getRevenueTrends(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "DAILY") String interval) {
        return ResponseEntity.ok(reportsService.getRevenueTrends(startDate, endDate, interval));
    }

    @GetMapping("/export/csv")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(defaultValue = "shipments") String reportType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Authentication auth) {
        if ("financial".equalsIgnoreCase(reportType) && !hasFinancialRole(auth)) {
            return ResponseEntity.status(403).build();
        }

        byte[] csvBytes = reportsService.exportCsvReport(reportType, startDate, endDate);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + reportType + "_report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvBytes);
    }

    @GetMapping("/export/pdf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam(defaultValue = "summary") String reportType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Authentication auth) throws Exception {
        boolean includeFinancial = hasFinancialRole(auth);
        byte[] pdfBytes = reportsService.exportPdfReport(reportType, startDate, endDate, includeFinancial);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + reportType + "_report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    private boolean hasFinancialRole(Authentication auth) {
        if (auth == null) return false;
        for (GrantedAuthority ga : auth.getAuthorities()) {
            String role = ga.getAuthority();
            if ("ROLE_ADMIN".equals(role) || "ROLE_PORT_MANAGER".equals(role) || "ROLE_FINANCE_OFFICER".equals(role)) {
                return true;
            }
        }
        return false;
    }
}
