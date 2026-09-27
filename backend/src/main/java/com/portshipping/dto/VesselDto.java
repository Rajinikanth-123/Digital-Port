package com.portshipping.dto;
import jakarta.validation.constraints.NotBlank;
public record VesselDto(@NotBlank String name,@NotBlank String imoNumber,@NotBlank String vesselType,String flag,Integer capacity,String operatorName,Double length,Double draft,String status) {}
