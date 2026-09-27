package com.portshipping.repository;

import com.portshipping.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByReference(String reference);

    boolean existsByReference(String reference);

    List<Payment> findByInvoiceId(Long invoiceId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.invoice.id = :invoiceId")
    BigDecimal sumAmountByInvoiceId(@Param("invoiceId") Long invoiceId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p")
    BigDecimal sumTotalPayments();

    Page<Payment> findByReferenceContainingIgnoreCaseOrPaymentMethodContainingIgnoreCase(String refQuery, String methodQuery, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE (:search IS NULL OR LOWER(p.reference) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.paymentMethod) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.invoice.invoiceNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Payment> searchPayments(@Param("search") String search, Pageable pageable);
}
