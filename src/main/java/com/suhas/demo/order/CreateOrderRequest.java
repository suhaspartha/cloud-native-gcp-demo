package com.suhas.demo.order;

import jakarta.validation.constraints.*;

public record CreateOrderRequest(
    @NotBlank @Size(max = 255) String item,
    @Min(1) int quantity
) {}