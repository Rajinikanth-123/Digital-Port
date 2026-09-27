package com.portshipping.repository;

import com.portshipping.entity.ShippingSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShippingScheduleRepository extends JpaRepository<ShippingSchedule, Long> {

    List<ShippingSchedule> findByVesselId(Long vesselId);
    List<ShippingSchedule> findByStatusIgnoreCase(String status);
    long countByVesselId(Long vesselId);
    long countByBerthId(Long berthId);

    @Query("SELECT s FROM ShippingSchedule s WHERE " +
           "(:search IS NULL OR LOWER(s.vessel.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           " OR LOWER(s.vessel.imoNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
           " OR LOWER(s.status) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:status IS NULL OR LOWER(s.status) = LOWER(:status)) " +
           "AND (:vesselId IS NULL OR s.vessel.id = :vesselId) " +
           "AND (:berthId IS NULL OR s.berth.id = :berthId)")
    Page<ShippingSchedule> searchSchedules(@Param("search") String search,
                                          @Param("status") String status,
                                          @Param("vesselId") Long vesselId,
                                          @Param("berthId") Long berthId,
                                          Pageable pageable);

    @Query("SELECT s FROM ShippingSchedule s WHERE s.berth.id = :berthId " +
           "AND s.status NOT IN ('CANCELLED', 'DEPARTED') " +
           "AND (:excludeId IS NULL OR s.id <> :excludeId) " +
           "AND (s.arrivalEstimated < :endTime AND (s.departureEstimated IS NULL OR s.departureEstimated > :startTime))")
    List<ShippingSchedule> findBerthScheduleConflicts(@Param("berthId") Long berthId,
                                                     @Param("startTime") LocalDateTime startTime,
                                                     @Param("endTime") LocalDateTime endTime,
                                                     @Param("excludeId") Long excludeId);

    @Query("SELECT s FROM ShippingSchedule s WHERE s.vessel.id = :vesselId " +
           "AND s.status NOT IN ('CANCELLED', 'DEPARTED') " +
           "AND (:excludeId IS NULL OR s.id <> :excludeId) " +
           "AND (s.arrivalEstimated < :endTime AND (s.departureEstimated IS NULL OR s.departureEstimated > :startTime))")
    List<ShippingSchedule> findVesselScheduleConflicts(@Param("vesselId") Long vesselId,
                                                      @Param("startTime") LocalDateTime startTime,
                                                      @Param("endTime") LocalDateTime endTime,
                                                      @Param("excludeId") Long excludeId);
}
