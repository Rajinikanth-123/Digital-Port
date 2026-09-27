package com.portshipping.dto;
import jakarta.validation.constraints.NotBlank;
public record ShipmentDto(@NotBlank String referenceNumber,String origin,String destination,String status,Integer containerCount,Long vesselId) {}
