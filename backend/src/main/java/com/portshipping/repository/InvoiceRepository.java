package com.portshipping.repository;

import com.portshipping.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    boolean existsByInvoiceNumber(String invoiceNumber);

    List<Invoice> findByPaymentStatusIgnoreCase(String status);

    List<Invoice> findByShipmentId(Long shipmentId);

    long countByShipmentId(Long shipmentId);

    Page<Invoice> findByInvoiceNumberContainingIgnoreCaseOrCustomerNameContainingIgnoreCase(String numberQuery, String nameQuery, Pageable pageable);

    @Query("SELECT i FROM Invoice i WHERE (:status IS NULL OR LOWER(i.paymentStatus) = LOWER(:status)) AND (:search IS NULL OR LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(i.customerName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Invoice> searchInvoices(@Param("search") String search, @Param("status") String status, Pageable pageable);

    @Query("SELECT COALESCE(SUM(i.charges + i.taxes - i.discount), 0) FROM Invoice i WHERE i.paymentStatus != 'CANCELLED'")
    BigDecimal calculateTotalInvoiced();

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.dueDate < :today AND i.paymentStatus NOT IN ('PAID', 'CANCELLED')")
    long countOverdueInvoices(@Param("today") LocalDate today);

    @Query("SELECT COALESCE(SUM(i.charges + i.taxes - i.discount), 0) FROM Invoice i WHERE i.dueDate < :today AND i.paymentStatus NOT IN ('PAID', 'CANCELLED')")
    BigDecimal sumOverdueAmount(@Param("today") LocalDate today);
}
