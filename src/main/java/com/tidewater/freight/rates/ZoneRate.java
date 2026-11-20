package com.tidewater.freight.rates;

/** The per-kilogram rate and fixed handling fee for a zone, in GHS. */
public record ZoneRate(double ratePerKg, double handlingFee) {
  /**
   * @throws IllegalArgumentException if the rate is not positive and finite, or the fee is
   *     negative or non-finite
   */
  public ZoneRate {
    if (!Double.isFinite(ratePerKg) || ratePerKg <= 0) {
      throw new IllegalArgumentException("Rate per kilogram must be positive and finite");
    }
    if (!Double.isFinite(handlingFee) || handlingFee < 0) {
      throw new IllegalArgumentException("Handling fee must be non-negative and finite");
    }
  }
}
