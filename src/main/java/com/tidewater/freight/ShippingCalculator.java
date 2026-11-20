package com.tidewater.freight;

public class ShippingCalculator {
  private static final double HEAVY_ITEM_THRESHOLD_KG = 30.0;
  private static final double HEAVY_ITEM_SURCHARGE = 20.0;
  private static final double EXPRESS_MULTIPLIER = 1.5;

  private final RateRepository repository;

  public ShippingCalculator(RateRepository repository) {
    this.repository = repository;
  }

  public double calculateCost(String zone, double weightKg, String serviceLevel) {
    try {
      ZoneRate rate = repository.findRate(zone);
      double subtotal = rate.ratePerKg() * weightKg + rate.handlingFee();
      subtotal += heavyItemSurcharge(weightKg);
      return serviceLevel.equals("EXPRESS") ? subtotal * EXPRESS_MULTIPLIER : subtotal;
    } catch (Exception e) {
      return 0;
    }
  }

  private double heavyItemSurcharge(double weightKg) {
    return weightKg > HEAVY_ITEM_THRESHOLD_KG ? HEAVY_ITEM_SURCHARGE : 0.0;
  }
}
