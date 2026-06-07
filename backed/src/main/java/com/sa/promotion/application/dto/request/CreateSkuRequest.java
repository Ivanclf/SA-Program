package com.sa.promotion.application.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSkuRequest {
    private String skuName;
    private BigDecimal originalPrice;
}
