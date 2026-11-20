package com.tidewater.freight.rates;

import com.tidewater.freight.exception.UnknownZoneException;
import java.math.BigDecimal;
import java.util.Map;

public class InMemoryRateRepository implements RateRepository {
  private final Map<String, ZoneRate> rates;

  public InMemoryRateRepository() {
    this(Map.of(
        "DOMESTIC", new ZoneRate(new BigDecimal("5.00"), new BigDecimal("0.00")),
        "REGIONAL", new ZoneRate(new BigDecimal("8.00"), new BigDecimal("10.00")),
        "INTERNATIONAL", new ZoneRate(new BigDecimal("15.00"), new BigDecimal("25.00"))));
  }

  public InMemoryRateRepository(Map<String, ZoneRate> rates) {
    this.rates = Map.copyOf(rates);
  }

  @Override
  public ZoneRate findRate(String zone) {
    ZoneRate rate = rates.get(zone);
    if (rate == null) {
      throw new UnknownZoneException(zone);
    }
    return rate;
  }
}
