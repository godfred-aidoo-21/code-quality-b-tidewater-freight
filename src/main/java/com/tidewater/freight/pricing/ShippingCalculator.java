package com.tidewater.freight.pricing;

import com.tidewater.freight.exception.InvalidShipmentException;
import com.tidewater.freight.exception.RateLookupException;
import com.tidewater.freight.exception.UnknownZoneException;
import com.tidewater.freight.rates.RateRepository;
import com.tidewater.freight.rates.ZoneRate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class ShippingCalculator {
  private static final double HEAVY_ITEM_THRESHOLD_KG = 30.0;
  private static final BigDecimal HEAVY_ITEM_SURCHARGE = new BigDecimal("20.00");
  private static final BigDecimal EXPRESS_MULTIPLIER = new BigDecimal("1.5");
  private static final int CURRENCY_SCALE = 2;

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
   * @return the final GHS amount, rounded once to two decimal places using HALF_UP
   */
  public BigDecimal calculateCost(String zone, double weightKg, String serviceLevel) {
    validateShipment(zone, weightKg, serviceLevel);
    ZoneRate rate = findRate(zone);
    BigDecimal subtotal = rate.ratePerKg().multiply(BigDecimal.valueOf(weightKg))
        .add(rate.handlingFee())
        .add(heavyItemSurcharge(weightKg));
    BigDecimal cost = serviceLevel.equals("EXPRESS")
        ? subtotal.multiply(EXPRESS_MULTIPLIER) : subtotal;
    return cost.setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
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

  private BigDecimal heavyItemSurcharge(double weightKg) {
    return weightKg > HEAVY_ITEM_THRESHOLD_KG ? HEAVY_ITEM_SURCHARGE : BigDecimal.ZERO;
  }
}
