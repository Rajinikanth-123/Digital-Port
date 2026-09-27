package com.portshipping.dto;
import jakarta.validation.constraints.NotBlank;
public record PortDto(@NotBlank String name,@NotBlank String code,@NotBlank String country,String timezone) {}
