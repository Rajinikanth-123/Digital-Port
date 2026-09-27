package com.portshipping.controller;

import com.portshipping.service.FinancialDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/financial")
public class FinancialController {

    private final FinancialDashboardService financialDashboardService;

    public FinancialController(FinancialDashboardService financialDashboardService) {
        this.financialDashboardService = financialDashboardService;
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getFinancialSummary() {
        return ResponseEntity.ok(financialDashboardService.getFinancialSummary());
    }
}
