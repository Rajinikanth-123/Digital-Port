package com.portshipping.repository;

import com.portshipping.entity.HelpArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HelpArticleRepository extends JpaRepository<HelpArticle, Long> {

    Optional<HelpArticle> findBySlug(String slug);

    Page<HelpArticle> findByPublishedTrue(Pageable pageable);

    Page<HelpArticle> findByPublishedTrueAndCategoryIgnoreCase(String category, Pageable pageable);

    Page<HelpArticle> findByPublishedTrueAndTitleContainingIgnoreCaseOrContentContainingIgnoreCase(String title, String content, Pageable pageable);
}
