package com.ems.academic.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record CheckoutRequest(UUID attemptId, BigDecimal amount, String currency, String description) {}