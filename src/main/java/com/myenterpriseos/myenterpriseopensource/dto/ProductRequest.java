package com.myenterpriseos.myenterpriseopensource.dto;

import java.math.BigDecimal;

public record ProductRequest (
    String name,
    String sku,
    BigDecimal price,
    String currency,
    Long companyId
){ }
