package com.portshipping.repository;

import com.portshipping.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n WHERE (n.user.id = :userId OR n.user IS NULL) " +
           "AND (:unreadOnly IS NULL OR :unreadOnly = false OR n.readStatus = false) " +
           "AND (:category IS NULL OR LOWER(n.type) = LOWER(:category)) " +
           "AND (:archived IS NULL OR n.archived = :archived) " +
           "ORDER BY n.createdAt DESC")
    Page<Notification> findFilteredForUser(@Param("userId") Long userId,
                                          @Param("unreadOnly") Boolean unreadOnly,
                                          @Param("category") String category,
                                          @Param("archived") Boolean archived,
                                          Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId OR n.user IS NULL ORDER BY n.createdAt DESC")
    Page<Notification> findForUser(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT COUNT(n) FROM Notification n WHERE (n.user.id = :userId OR n.user IS NULL) AND n.readStatus = false AND (n.archived = false OR n.archived IS NULL)")
    long countUnreadForUser(@Param("userId") Long userId);

    @Query("SELECT n FROM Notification n WHERE (n.user.id = :userId OR n.user IS NULL) AND n.readStatus = false")
    List<Notification> findAllUnreadForUser(@Param("userId") Long userId);

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId OR n.user IS NULL ORDER BY n.createdAt DESC")
    List<Notification> findAllForUser(@Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(n) > 0 THEN true ELSE false END FROM Notification n " +
           "WHERE (n.user.id = :userId OR (:userId IS NULL AND n.user IS NULL)) " +
           "AND n.title = :title AND n.entityName = :entityName AND n.entityId = :entityId " +
           "AND n.createdAt > :since")
    boolean existsDuplicate(@Param("userId") Long userId,
                            @Param("title") String title,
                            @Param("entityName") String entityName,
                            @Param("entityId") Long entityId,
                            @Param("since") Instant since);
}
