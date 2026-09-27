package com.portshipping.dto;
import jakarta.validation.constraints.NotBlank;
public record CargoDto(@NotBlank String description,String cargoType,Double weight,String unit) {}
