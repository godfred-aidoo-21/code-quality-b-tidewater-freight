package com.tidewater.freight.rates;

import com.tidewater.freight.exception.RateLookupException;
import com.tidewater.freight.exception.UnknownZoneException;

public interface RateRepository {
  /**
   * Returns non-null rules for the zone.
   *
   * @throws UnknownZoneException if the zone is not configured
   * @throws RateLookupException if rules cannot be retrieved from the rate source
   */
  ZoneRate findRate(String zone);
}
