package com.tidewater.freight;

import java.util.Map;

public class InMemoryRateRepository implements RateRepository {
  private final Map<String, ZoneRate> rates;

  public InMemoryRateRepository() {
    this(Map.of(
        "DOMESTIC", new ZoneRate(5.0, 0.0),
        "REGIONAL", new ZoneRate(8.0, 10.0),
        "INTERNATIONAL", new ZoneRate(15.0, 25.0)));
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
