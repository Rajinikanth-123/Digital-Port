package com.portshipping.dto;
import jakarta.validation.constraints.*;
public record TerminalDto(@NotNull Long portId,@NotBlank String name,@NotBlank String code,String terminalType,@PositiveOrZero Integer capacity) {}
