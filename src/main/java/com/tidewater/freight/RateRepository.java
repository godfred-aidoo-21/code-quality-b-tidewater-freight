package com.tidewater.freight;

public interface RateRepository {
  ZoneRate findRate(String zone);
}
