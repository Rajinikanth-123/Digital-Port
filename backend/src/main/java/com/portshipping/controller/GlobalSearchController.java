package com.portshipping.controller;

import com.portshipping.dto.GlobalSearchResponseDto;
import com.portshipping.dto.GlobalSearchResultDto;
import com.portshipping.dto.SearchFilterOptionsDto;
import com.portshipping.service.GlobalSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class GlobalSearchController {

    private final GlobalSearchService globalSearchService;

    public GlobalSearchController(GlobalSearchService globalSearchService) {
        this.globalSearchService = globalSearchService;
    }

    @GetMapping
    public ResponseEntity<GlobalSearchResponseDto> search(
            @RequestParam(name = "q", required = false, defaultValue = "") String query,
            @RequestParam(name = "module", required = false) String module,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            @RequestParam(name = "portId", required = false) Long portId,
            @RequestParam(name = "vesselId", required = false) Long vesselId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sortBy", defaultValue = "eventDate") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir
    ) {
        GlobalSearchResponseDto response = globalSearchService.search(
                query, module, status, startDate, endDate, portId, vesselId, page, size, sortBy, sortDir
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/filters")
    public ResponseEntity<SearchFilterOptionsDto> getFilters() {
        SearchFilterOptionsDto options = globalSearchService.getFilterOptions();
        return ResponseEntity.ok(options);
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<GlobalSearchResultDto>> getSuggestions(
            @RequestParam(name = "q", required = false, defaultValue = "") String query,
            @RequestParam(name = "limit", defaultValue = "5") int limit
    ) {
        List<GlobalSearchResultDto> suggestions = globalSearchService.getSuggestions(query, limit);
        return ResponseEntity.ok(suggestions);
    }
}
