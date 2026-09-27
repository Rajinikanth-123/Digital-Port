package com.portshipping.dto;
import jakarta.validation.constraints.NotBlank;
public record ShippingScheduleDto(@NotBlank String vesselName,String voyageNumber,String origin,String destination,String status) {}
