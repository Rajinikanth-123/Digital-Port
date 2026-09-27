package com.portshipping.service;

import com.portshipping.dto.GlobalSearchResponseDto;
import com.portshipping.dto.GlobalSearchResultDto;
import com.portshipping.dto.SearchFilterOptionsDto;
import com.portshipping.entity.*;
import com.portshipping.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class GlobalSearchService {

    private final ShipmentRepository shipmentRepository;
    private final VesselRepository vesselRepository;
    private final ContainerRepository containerRepository;
    private final PortRepository portRepository;
    private final TerminalRepository terminalRepository;
    private final CustomsClearanceRepository customsClearanceRepository;
    private final DocumentRepository documentRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    public GlobalSearchService(ShipmentRepository shipmentRepository,
                               VesselRepository vesselRepository,
                               ContainerRepository containerRepository,
                               PortRepository portRepository,
                               TerminalRepository terminalRepository,
                               CustomsClearanceRepository customsClearanceRepository,
                               DocumentRepository documentRepository,
                               InvoiceRepository invoiceRepository,
                               PaymentRepository paymentRepository,
                               UserRepository userRepository) {
        this.shipmentRepository = shipmentRepository;
        this.vesselRepository = vesselRepository;
        this.containerRepository = containerRepository;
        this.portRepository = portRepository;
        this.terminalRepository = terminalRepository;
        this.customsClearanceRepository = customsClearanceRepository;
        this.documentRepository = documentRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
    }

    public List<String> getPermittedModulesForUser(String role) {
        String cleanRole = (role != null) ? role.toUpperCase().replace("ROLE_", "") : "VIEWER";
        switch (cleanRole) {
            case "ADMIN":
                return List.of("SHIPMENT", "VESSEL", "CONTAINER", "PORT", "TERMINAL", "CUSTOMS", "DOCUMENT", "INVOICE", "PAYMENT", "USER");
            case "PORT_MANAGER":
                return List.of("SHIPMENT", "VESSEL", "CONTAINER", "PORT", "TERMINAL", "CUSTOMS", "DOCUMENT", "INVOICE", "PAYMENT");
            case "SHIPPING_AGENT":
                return List.of("SHIPMENT", "VESSEL", "CONTAINER", "CUSTOMS", "DOCUMENT");
            case "FINANCE_OFFICER":
                return List.of("SHIPMENT", "INVOICE", "PAYMENT");
            case "VIEWER":
            default:
                return List.of("SHIPMENT", "VESSEL", "CONTAINER", "PORT", "TERMINAL");
        }
    }

    public String getCurrentUserRole() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                User u = userRepository.findByEmailIgnoreCase(auth.getName()).orElse(null);
                if (u != null) return u.getRole();
            }
        } catch (Exception ignored) {}
        return "VIEWER";
    }

    public SearchFilterOptionsDto getFilterOptions() {
        String role = getCurrentUserRole();
        List<String> modules = getPermittedModulesForUser(role);
        List<String> statuses = List.of(
                "REGISTERED", "IN_TRANSIT", "DELIVERED", "CANCELLED",
                "PENDING", "APPROVED", "REJECTED",
                "ISSUED", "PAID", "OVERDUE", "PARTIAL",
                "ACTIVE", "INACTIVE"
        );
        return new SearchFilterOptionsDto(modules, statuses);
    }

    public GlobalSearchResponseDto search(String rawQuery, String moduleFilter, String statusFilter,
                                          String startDateStr, String endDateStr,
                                          Long portId, Long vesselId,
                                          int page, int size, String sortBy, String sortDir) {
        String query = sanitizeQuery(rawQuery);
        if (query.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search query exceeds maximum allowed length of 100 characters");
        }

        Instant startInstant = parseDateBoundary(startDateStr, true);
        Instant endInstant = parseDateBoundary(endDateStr, false);

        if (startInstant != null && endInstant != null && startInstant.isAfter(endInstant)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date cannot be after end date");
        }

        int pageNum = Math.max(0, page);
        int pageSize = Math.min(Math.max(1, size), 50);

        String userRole = getCurrentUserRole();
        List<String> permittedModules = getPermittedModulesForUser(userRole);

        if (moduleFilter != null && !moduleFilter.isBlank() && !"ALL".equalsIgnoreCase(moduleFilter)) {
            String targetModule = moduleFilter.toUpperCase().trim();
            if (!permittedModules.contains(targetModule)) {
                return new GlobalSearchResponseDto(rawQuery, 0, 0, pageNum, pageSize, Map.of(), List.of());
            }
            permittedModules = List.of(targetModule);
        }

        List<GlobalSearchResultDto> allResults = new ArrayList<>();
        Map<String, Long> moduleCounts = new LinkedHashMap<>();

        for (String mod : permittedModules) {
            List<GlobalSearchResultDto> modResults = searchModule(mod, query, statusFilter, startInstant, endInstant, portId, vesselId);
            moduleCounts.put(mod, (long) modResults.size());
            allResults.addAll(modResults);
        }

        sortResults(allResults, sortBy, sortDir);

        int totalElements = allResults.size();
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        int fromIndex = Math.min(pageNum * pageSize, totalElements);
        int toIndex = Math.min(fromIndex + pageSize, totalElements);

        List<GlobalSearchResultDto> pagedItems = allResults.subList(fromIndex, toIndex);

        return new GlobalSearchResponseDto(rawQuery != null ? rawQuery : "", totalElements, totalPages, pageNum, pageSize, moduleCounts, pagedItems);
    }

    public List<GlobalSearchResultDto> getSuggestions(String rawQuery, int limit) {
        String query = sanitizeQuery(rawQuery);
        if (query == null || query.isBlank() || query.length() < 2) {
            return List.of();
        }
        GlobalSearchResponseDto response = search(query, null, null, null, null, null, null, 0, Math.min(limit, 10), "eventDate", "desc");
        return response.getItems();
    }

    private List<GlobalSearchResultDto> searchModule(String module, String query, String statusFilter,
                                                      Instant startInstant, Instant endInstant,
                                                      Long portId, Long vesselId) {
        List<GlobalSearchResultDto> list = new ArrayList<>();
        String likePattern = (query != null && !query.isBlank()) ? "%" + escapeLikePattern(query.toLowerCase()) + "%" : null;
        String status = (statusFilter != null && !statusFilter.isBlank() && !"ALL".equalsIgnoreCase(statusFilter)) ? statusFilter.toUpperCase().trim() : null;

        switch (module) {
            case "SHIPMENT":
                for (Shipment s : shipmentRepository.findAll()) {
                    if (matchText(likePattern, s.getTrackingNumber(), s.getReferenceNumber(), s.getOrigin(), s.getDestination(), s.getShipper(), s.getConsignee()) &&
                        matchStatus(status, s.getStatus()) &&
                        matchDate(startInstant, endInstant, s.getCreatedAt())) {
                        list.add(new GlobalSearchResultDto(
                                s.getId(), "SHIPMENT",
                                "Shipment " + s.getTrackingNumber(),
                                s.getTrackingNumber(),
                                s.getStatus(),
                                s.getCreatedAt(),
                                (s.getShipper() != null ? s.getShipper() : "") + " → " + (s.getConsignee() != null ? s.getConsignee() : "") + " (" + s.getOrigin() + " to " + s.getDestination() + ")",
                                "shipments.html?id=" + s.getId(),
                                Map.of("origin", s.getOrigin() != null ? s.getOrigin() : "", "destination", s.getDestination() != null ? s.getDestination() : "")
                        ));
                    }
                }
                break;

            case "VESSEL":
                for (Vessel v : vesselRepository.findAll()) {
                    if (matchText(likePattern, v.getName(), v.getImoNumber(), v.getFlag(), v.getVesselType(), v.getOperatorName()) &&
                        matchStatus(status, v.getStatus()) &&
                        matchDate(startInstant, endInstant, v.getCreatedAt())) {
                        list.add(new GlobalSearchResultDto(
                                v.getId(), "VESSEL",
                                v.getName(),
                                v.getImoNumber(),
                                v.getStatus(),
                                v.getCreatedAt(),
                                "IMO: " + v.getImoNumber() + " | Flag: " + v.getFlag() + " | Type: " + v.getVesselType(),
                                "vessels.html?id=" + v.getId(),
                                Map.of("flag", v.getFlag() != null ? v.getFlag() : "", "type", v.getVesselType() != null ? v.getVesselType() : "")
                        ));
                    }
                }
                break;

            case "CONTAINER":
                for (Container c : containerRepository.findAll()) {
                    if (matchText(likePattern, c.getContainerNumber(), c.getSize(), c.getType(), c.getLocation()) &&
                        matchStatus(status, c.getStatus()) &&
                        matchDate(startInstant, endInstant, c.getCreatedAt())) {
                        list.add(new GlobalSearchResultDto(
                                c.getId(), "CONTAINER",
                                "Container " + c.getContainerNumber(),
                                c.getContainerNumber(),
                                c.getStatus(),
                                c.getCreatedAt(),
                                "Size: " + c.getSize() + " | Type: " + c.getType() + " | Location: " + (c.getLocation() != null ? c.getLocation() : "N/A"),
                                "containers.html?id=" + c.getId(),
                                Map.of("size", c.getSize() != null ? c.getSize() : "", "type", c.getType() != null ? c.getType() : "")
                        ));
                    }
                }
                break;

            case "PORT":
                for (Port p : portRepository.findAll()) {
                    if (matchText(likePattern, p.getName(), p.getCode(), p.getCountry()) &&
                        matchDate(startInstant, endInstant, p.getCreatedAt())) {
                        list.add(new GlobalSearchResultDto(
                                p.getId(), "PORT",
                                p.getName() + " (" + p.getCode() + ")",
                                p.getCode(),
                                "ACTIVE",
                                p.getCreatedAt(),
                                "Country: " + p.getCountry() + " | Code: " + p.getCode(),
                                "ports.html?id=" + p.getId(),
                                Map.of("country", p.getCountry() != null ? p.getCountry() : "")
                        ));
                    }
                }
                break;

            case "TERMINAL":
                for (Terminal t : terminalRepository.findAll()) {
                    if (matchText(likePattern, t.getName(), t.getCode(), t.getTerminalType()) &&
                        matchDate(startInstant, endInstant, t.getCreatedAt())) {
                        list.add(new GlobalSearchResultDto(
                                t.getId(), "TERMINAL",
                                t.getName(),
                                t.getCode(),
                                "ACTIVE",
                                t.getCreatedAt(),
                                "Code: " + t.getCode() + " | Type: " + t.getTerminalType(),
                                "terminals.html?id=" + t.getId(),
                                Map.of("type", t.getTerminalType() != null ? t.getTerminalType() : "")
                        ));
                    }
                }
                break;

            case "CUSTOMS":
                for (CustomsClearance cc : customsClearanceRepository.findAll()) {
                    Instant submittedInstant = cc.getSubmittedAt() != null ? cc.getSubmittedAt().toInstant(ZoneOffset.UTC) : cc.getCreatedAt();
                    if (matchText(likePattern, cc.getDeclarationNumber(), cc.getRemarks(), cc.getDocumentDetails()) &&
                        matchStatus(status, cc.getStatus()) &&
                        matchDate(startInstant, endInstant, submittedInstant)) {
                        list.add(new GlobalSearchResultDto(
                                cc.getId(), "CUSTOMS",
                                "Customs " + cc.getDeclarationNumber(),
                                cc.getDeclarationNumber(),
                                cc.getStatus(),
                                submittedInstant,
                                "Declaration: " + cc.getDeclarationNumber() + " | Status: " + cc.getStatus(),
                                "customs.html?id=" + cc.getId(),
                                Map.of("declaration", cc.getDeclarationNumber() != null ? cc.getDeclarationNumber() : "")
                        ));
                    }
                }
                break;

            case "DOCUMENT":
                for (Document d : documentRepository.findAll()) {
                    if (matchText(likePattern, d.getFileName(), d.getDocumentType(), d.getMimeType()) &&
                        matchDate(startInstant, endInstant, d.getUploadedAt())) {
                        list.add(new GlobalSearchResultDto(
                                d.getId(), "DOCUMENT",
                                d.getFileName(),
                                d.getFileName(),
                                "ACTIVE",
                                d.getUploadedAt(),
                                "File: " + d.getFileName() + " | Type: " + d.getDocumentType() + " | Size: " + d.getFileSizeBytes() + " bytes",
                                "documents.html?id=" + d.getId(),
                                Map.of("fileName", d.getFileName() != null ? d.getFileName() : "")
                        ));
                    }
                }
                break;

            case "INVOICE":
                for (Invoice inv : invoiceRepository.findAll()) {
                    if (matchText(likePattern, inv.getInvoiceNumber(), inv.getCustomerName(), inv.getCurrency()) &&
                        matchStatus(status, inv.getPaymentStatus()) &&
                        matchDate(startInstant, endInstant, inv.getCreatedAt())) {
                        list.add(new GlobalSearchResultDto(
                                inv.getId(), "INVOICE",
                                "Invoice " + inv.getInvoiceNumber(),
                                inv.getInvoiceNumber(),
                                inv.getPaymentStatus(),
                                inv.getCreatedAt(),
                                "Customer: " + inv.getCustomerName() + " | Amount: " + inv.getCurrency() + " " + inv.getCharges(),
                                "invoices.html?id=" + inv.getId(),
                                Map.of("customer", inv.getCustomerName() != null ? inv.getCustomerName() : "")
                        ));
                    }
                }
                break;

            case "PAYMENT":
                for (Payment p : paymentRepository.findAll()) {
                    Instant paidInstant = p.getPaidAt() != null ? p.getPaidAt().toInstant(ZoneOffset.UTC) : null;
                    if (matchText(likePattern, p.getReference(), p.getPaymentMethod()) &&
                        matchDate(startInstant, endInstant, paidInstant)) {
                        list.add(new GlobalSearchResultDto(
                                p.getId(), "PAYMENT",
                                "Payment " + (p.getReference() != null ? p.getReference() : p.getId()),
                                p.getReference(),
                                "COMPLETED",
                                paidInstant,
                                "Method: " + p.getPaymentMethod() + " | Amount: " + p.getAmount(),
                                "payments.html?id=" + p.getId(),
                                Map.of("method", p.getPaymentMethod() != null ? p.getPaymentMethod() : "")
                        ));
                    }
                }
                break;

            case "USER":
                for (User u : userRepository.findAll()) {
                    if (matchText(likePattern, u.getName(), u.getEmail(), u.getRole()) &&
                        matchDate(startInstant, endInstant, u.getCreatedAt())) {
                        list.add(new GlobalSearchResultDto(
                                u.getId(), "USER",
                                u.getName(),
                                u.getEmail(),
                                u.isEnabled() ? "ACTIVE" : "INACTIVE",
                                u.getCreatedAt(),
                                "Role: " + u.getRole() + " | Email: " + u.getEmail(),
                                "users.html?id=" + u.getId(),
                                Map.of("email", u.getEmail() != null ? u.getEmail() : "")
                        ));
                    }
                }
                break;
        }

        return list;
    }

    private boolean matchText(String likePattern, String... fields) {
        if (likePattern == null) return true;
        String cleanPattern = likePattern.substring(1, likePattern.length() - 1).replace("\\%", "%").replace("\\_", "_");
        for (String f : fields) {
            if (f != null && f.toLowerCase().contains(cleanPattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchStatus(String statusFilter, String itemStatus) {
        if (statusFilter == null) return true;
        if (itemStatus == null) return false;
        return itemStatus.equalsIgnoreCase(statusFilter);
    }

    private boolean matchDate(Instant start, Instant end, Instant date) {
        if (date == null) return (start == null && end == null);
        if (start != null && date.isBefore(start)) return false;
        if (end != null && date.isAfter(end)) return false;
        return true;
    }

    private void sortResults(List<GlobalSearchResultDto> results, String sortBy, String sortDir) {
        boolean isAsc = "asc".equalsIgnoreCase(sortDir);
        String field = (sortBy != null) ? sortBy.toLowerCase().trim() : "eventdate";

        Comparator<GlobalSearchResultDto> comparator;
        switch (field) {
            case "title":
                comparator = Comparator.comparing(r -> r.getDisplayTitle() != null ? r.getDisplayTitle().toLowerCase() : "");
                break;
            case "module":
                comparator = Comparator.comparing(r -> r.getModule() != null ? r.getModule() : "");
                break;
            case "status":
                comparator = Comparator.comparing(r -> r.getStatus() != null ? r.getStatus() : "");
                break;
            case "id":
                comparator = Comparator.comparing(GlobalSearchResultDto::getRecordId, Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "eventdate":
            default:
                comparator = Comparator.comparing(GlobalSearchResultDto::getEventDate, Comparator.nullsLast(Comparator.naturalOrder()));
                break;
        }

        if (!isAsc) {
            comparator = comparator.reversed();
        }

        comparator = comparator.thenComparing(GlobalSearchResultDto::getRecordId, Comparator.nullsLast(Comparator.naturalOrder()));
        results.sort(comparator);
    }

    private String sanitizeQuery(String q) {
        if (q == null) return "";
        return q.trim();
    }

    private String escapeLikePattern(String str) {
        if (str == null) return "";
        return str.replace("%", "\\%").replace("_", "\\_");
    }

    private Instant parseDateBoundary(String dateStr, boolean isStart) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            if (dateStr.contains("T")) {
                return Instant.parse(dateStr);
            }
            LocalDate ld = LocalDate.parse(dateStr);
            return isStart ? ld.atStartOfDay(ZoneOffset.UTC).toInstant() : ld.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusMillis(1);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date format: '" + dateStr + "'. Expected ISO-8601 (YYYY-MM-DD)");
        }
    }
}
