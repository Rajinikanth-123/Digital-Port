package com.portshipping.service;

import com.portshipping.dto.HelpArticleDto;
import com.portshipping.entity.HelpArticle;
import com.portshipping.repository.HelpArticleRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class HelpCenterService {

    private final HelpArticleRepository helpArticleRepository;

    public HelpCenterService(HelpArticleRepository helpArticleRepository) {
        this.helpArticleRepository = helpArticleRepository;
    }

    @PostConstruct
    public void initSeedArticles() {
        if (helpArticleRepository.count() == 0) {
            seedArticles();
        }
    }

    @Transactional(readOnly = true)
    public Page<HelpArticleDto> searchArticles(String search, String category, Pageable pageable) {
        ensureArticlesSeeded();

        Page<HelpArticle> page;
        if (search != null && !search.isBlank()) {
            page = helpArticleRepository.findByPublishedTrueAndTitleContainingIgnoreCaseOrContentContainingIgnoreCase(search.trim(), search.trim(), pageable);
        } else if (category != null && !category.isBlank() && !"ALL".equalsIgnoreCase(category)) {
            page = helpArticleRepository.findByPublishedTrueAndCategoryIgnoreCase(category.trim(), pageable);
        } else {
            page = helpArticleRepository.findByPublishedTrue(pageable);
        }

        List<HelpArticleDto> dtos = page.getContent().stream().map(HelpArticleDto::fromEntity).toList();
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public HelpArticleDto getArticleById(Long id) {
        HelpArticle article = helpArticleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Help article #" + id + " not found."));
        return HelpArticleDto.fromEntity(article);
    }

    @Transactional(readOnly = true)
    public List<HelpArticleDto> getFaqs() {
        ensureArticlesSeeded();
        return helpArticleRepository.findByPublishedTrue(Pageable.ofSize(10)).getContent()
                .stream().map(HelpArticleDto::fromEntity).toList();
    }

    private void ensureArticlesSeeded() {
        if (helpArticleRepository.count() == 0) {
            seedArticles();
        }
    }

    private void seedArticles() {
        List<HelpArticle> seedList = List.of(
            new HelpArticle("Tracking and Shipment Statuses", "shipment-tracking-guide", "SHIPMENTS",
                    "Shipment statuses progress from ORDERED to IN_TRANSIT, DELIVERED, or ON_HOLD. You can search shipments using tracking numbers or filter by origin port and destination port in the Shipment Tracking module.", null),
            new HelpArticle("Customs Clearance and Declaration Documents", "customs-clearance-guide", "CUSTOMS",
                    "Declarations submitted to customs require attached commercial invoices and bills of lading. Customs clearance officers review declarations and mark status as APPROVED, REJECTED, or PENDING_DOCUMENTS.", null),
            new HelpArticle("Managing Invoices, Taxes, and Payments", "invoicing-and-payment-guide", "BILLING",
                    "Invoices automatically compute port usage fees, berth charges, and taxes. Payments can be submitted via credit card or wire transfer and are verified against invoice balances.", null),
            new HelpArticle("User Profile Settings and Password Updates", "profile-and-account-settings", "MY_ACCOUNT",
                    "Update your display name, contact phone number, preferred theme (Light/Dark), and in-app notification alerts directly under your Account Profile workspace.", null),
            new HelpArticle("Vessel Arrival Windows and Berth Scheduling", "vessel-berth-scheduling", "VESSELS",
                    "Port operations schedule vessel arrival windows and allocate berths without conflicting timeframes. Berth managers monitor draft limits and quay lengths in real-time.", null)
        );
        helpArticleRepository.saveAll(seedList);
    }
}
