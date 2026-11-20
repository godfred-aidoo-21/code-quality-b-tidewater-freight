package com.tidewater.freight.rates;

import java.math.BigDecimal;

/** Exact per-kilogram rate and fixed handling fee for a zone, in GHS. */
public record ZoneRate(BigDecimal ratePerKg, BigDecimal handlingFee) {
  /** @throws IllegalArgumentException if the rate is null/non-positive or the fee null/negative */
  public ZoneRate {
    if (ratePerKg == null || ratePerKg.signum() <= 0) {
      throw new IllegalArgumentException("Rate per kilogram must be present and positive");
    }
    if (handlingFee == null || handlingFee.signum() < 0) {
      throw new IllegalArgumentException("Handling fee must be present and non-negative");
    }
  }
}
