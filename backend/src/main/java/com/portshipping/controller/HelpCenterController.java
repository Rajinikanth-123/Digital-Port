package com.portshipping.controller;

import com.portshipping.dto.HelpArticleDto;
import com.portshipping.service.HelpCenterService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class HelpCenterController {

    private final HelpCenterService helpCenterService;

    public HelpCenterController(HelpCenterService helpCenterService) {
        this.helpCenterService = helpCenterService;
    }

    @GetMapping("/api/help/articles")
    public ResponseEntity<Page<HelpArticleDto>> searchArticles(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("title").ascending());
        return ResponseEntity.ok(helpCenterService.searchArticles(search, category, pageable));
    }

    @GetMapping("/api/help/articles/{id}")
    public ResponseEntity<HelpArticleDto> getArticleById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(helpCenterService.getArticleById(id));
    }

    @GetMapping("/api/help/faqs")
    public ResponseEntity<List<HelpArticleDto>> getFaqs() {
        return ResponseEntity.ok(helpCenterService.getFaqs());
    }
}
