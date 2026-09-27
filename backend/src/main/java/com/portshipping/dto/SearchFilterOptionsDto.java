package com.portshipping.dto;

import java.util.List;

public class SearchFilterOptionsDto {
    private List<String> availableModules;
    private List<String> availableStatuses;

    public SearchFilterOptionsDto() {}

    public SearchFilterOptionsDto(List<String> availableModules, List<String> availableStatuses) {
        this.availableModules = availableModules;
        this.availableStatuses = availableStatuses;
    }

    public List<String> getAvailableModules() { return availableModules; }
    public void setAvailableModules(List<String> availableModules) { this.availableModules = availableModules; }

    public List<String> getAvailableStatuses() { return availableStatuses; }
    public void setAvailableStatuses(List<String> availableStatuses) { this.availableStatuses = availableStatuses; }
}
