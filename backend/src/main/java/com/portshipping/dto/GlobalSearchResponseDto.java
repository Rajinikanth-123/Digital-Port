package com.portshipping.dto;

import java.util.List;
import java.util.Map;

public class GlobalSearchResponseDto {
    private String query;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
    private Map<String, Long> moduleCounts;
    private List<GlobalSearchResultDto> items;

    public GlobalSearchResponseDto() {}

    public GlobalSearchResponseDto(String query, long totalElements, int totalPages, int currentPage, int pageSize,
                                   Map<String, Long> moduleCounts, List<GlobalSearchResultDto> items) {
        this.query = query;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.moduleCounts = moduleCounts;
        this.items = items;
    }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }

    public Map<String, Long> getModuleCounts() { return moduleCounts; }
    public void setModuleCounts(Map<String, Long> moduleCounts) { this.moduleCounts = moduleCounts; }

    public List<GlobalSearchResultDto> getItems() { return items; }
    public void setItems(List<GlobalSearchResultDto> items) { this.items = items; }
}
