package com.portshipping.dto;
import jakarta.validation.constraints.*;
public record BerthDto(@NotNull Long terminalId,@NotBlank String name,@NotNull @Positive Double maxLengthM,String status) {}
