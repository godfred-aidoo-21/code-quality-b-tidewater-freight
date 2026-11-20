package com.tidewater.freight;

public interface RateRepository {
  /**
   * Returns non-null rules for the zone.
   *
   * @throws UnknownZoneException if the zone is not configured
   * @throws RateLookupException if rules cannot be retrieved from the rate source
   */
  ZoneRate findRate(String zone);
}
