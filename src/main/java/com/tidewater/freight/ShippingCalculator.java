package com.tidewater.freight;

import java.util.Objects;

public class ShippingCalculator {
  private static final double HEAVY_ITEM_THRESHOLD_KG = 30.0;
  private static final double HEAVY_ITEM_SURCHARGE = 20.0;
  private static final double EXPRESS_MULTIPLIER = 1.5;

  private final RateRepository repository;

  public ShippingCalculator(RateRepository repository) {
    this.repository = Objects.requireNonNull(repository, "Rate repository is required");
  }

  /**
   * Prices a positive, finite weight using a configured zone and STANDARD or EXPRESS service.
   *
   * @throws InvalidShipmentException if shipment details are invalid
   * @throws UnknownZoneException if the zone is not configured
   * @throws RateLookupException if the rate source cannot supply rules
   * @throws ArithmeticException if the calculated amount is not finite
   */
  public double calculateCost(String zone, double weightKg, String serviceLevel) {
    validateShipment(zone, weightKg, serviceLevel);
    ZoneRate rate = findRate(zone);
    double subtotal = rate.ratePerKg() * weightKg + rate.handlingFee();
    subtotal += heavyItemSurcharge(weightKg);
    double cost = serviceLevel.equals("EXPRESS") ? subtotal * EXPRESS_MULTIPLIER : subtotal;
    if (!Double.isFinite(cost)) {
      throw new ArithmeticException("Shipping cost is not finite for zone " + zone);
    }
    return cost;
  }

  private void validateShipment(String zone, double weightKg, String serviceLevel) {
    if (zone == null || zone.isBlank()) {
      throw new InvalidShipmentException("Shipping zone is required");
    }
    if (!Double.isFinite(weightKg) || weightKg <= 0) {
      throw new InvalidShipmentException("Weight must be positive and finite, in kilograms");
    }
    if (!"STANDARD".equals(serviceLevel) && !"EXPRESS".equals(serviceLevel)) {
      throw new InvalidShipmentException("Service level must be STANDARD or EXPRESS");
    }
  }

  private ZoneRate findRate(String zone) {
    try {
      ZoneRate rate = repository.findRate(zone);
      if (rate == null) {
        throw new RateLookupException("Rate repository returned no rules");
      }
      return rate;
    } catch (RateLookupException e) {
      throw new RateLookupException("Could not retrieve shipping rules for zone " + zone, e);
    }
  }

  private double heavyItemSurcharge(double weightKg) {
    return weightKg > HEAVY_ITEM_THRESHOLD_KG ? HEAVY_ITEM_SURCHARGE : 0.0;
  }
}
