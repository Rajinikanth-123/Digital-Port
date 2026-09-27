package com.portshipping.dto;
import jakarta.validation.constraints.NotBlank;
public record ContainerDto(@NotBlank String containerNumber,String size,String type,String status,Long shipmentId) {}
