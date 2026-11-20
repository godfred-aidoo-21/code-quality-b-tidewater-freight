package com.tidewater.freight.rates;

import com.tidewater.freight.exception.UnknownZoneException;
import java.math.BigDecimal;
import java.util.Map;

public class InMemoryRateRepository implements RateRepository {
  private final Map<String, ZoneRate> rates;

  public InMemoryRateRepository() {
    this(Map.of(
        "DOMESTIC", new ZoneRate(new BigDecimal("5.0"), new BigDecimal("0.0")),
        "REGIONAL", new ZoneRate(new BigDecimal("8.0"), new BigDecimal("10.0")),
        "INTERNATIONAL", new ZoneRate(new BigDecimal("15.0"), new BigDecimal("25.0"))));
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
