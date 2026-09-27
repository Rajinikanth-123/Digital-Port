package com.portshipping.repository;

import com.portshipping.entity.BerthAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;

public interface BerthAllocationRepository extends JpaRepository<BerthAllocation, Long> {
    
    List<BerthAllocation> findByVesselId(Long vesselId);
    List<BerthAllocation> findByBerthEntityId(Long berthId);

    long countByVesselId(Long vesselId);
    long countByBerthEntityId(Long berthId);

    @Query("select count(b)>0 from BerthAllocation b where " +
           "((:berthId is not null and b.berthEntity.id = :berthId) or (:berthName is not null and LOWER(b.berth) = LOWER(:berthName))) " +
           "and b.startTime < :endTime and b.endTime > :startTime " +
           "and b.status != 'CANCELLED'")
    boolean hasConflict(@Param("berthId") Long berthId, @Param("berthName") String berthName,
                        @Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    @Query("select count(b)>0 from BerthAllocation b where " +
           "((:berthId is not null and b.berthEntity.id = :berthId) or (:berthName is not null and LOWER(b.berth) = LOWER(:berthName))) " +
           "and b.startTime < :endTime and b.endTime > :startTime " +
           "and b.status != 'CANCELLED' " +
           "and (:excludeId is null or b.id != :excludeId)")
    boolean hasConflictExcluding(@Param("berthId") Long berthId, @Param("berthName") String berthName,
                                 @Param("startTime") Instant startTime, @Param("endTime") Instant endTime,
                                 @Param("excludeId") Long excludeId);
}
