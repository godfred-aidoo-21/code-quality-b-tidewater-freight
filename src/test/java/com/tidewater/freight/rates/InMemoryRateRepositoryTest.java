package com.tidewater.freight.rates;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tidewater.freight.exception.UnknownZoneException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class InMemoryRateRepositoryTest {
  @ParameterizedTest
  @CsvSource({"DOMESTIC, 5.0, 0.0", "REGIONAL, 8.0, 10.0", "INTERNATIONAL, 15.0, 25.0"})
  void providesTheConfiguredRateAndHandlingFee(
      String zone, BigDecimal expectedRatePerKg, BigDecimal expectedHandlingFee) {
    RateRepository repository = new InMemoryRateRepository();

    ZoneRate rate = repository.findRate(zone);

    assertAll(
        () -> assertEquals(expectedRatePerKg, rate.ratePerKg()),
        () -> assertEquals(expectedHandlingFee, rate.handlingFee()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"MOON", "domestic", " DOMESTIC "})
  void reportsTheRequestedZoneWhenNoRuleMatches(String zone) {
    RateRepository repository = new InMemoryRateRepository();

    UnknownZoneException failure = assertThrows(UnknownZoneException.class,
        () -> repository.findRate(zone));

    assertEquals("Unknown shipping zone: " + zone, failure.getMessage());
  }

  @Test
  void keepsItsRulesWhenTheCallerChangesTheOriginalMap() {
    Map<String, ZoneRate> configuredRates = new HashMap<>();
    configuredRates.put("DOMESTIC", new ZoneRate(new BigDecimal("5"), new BigDecimal("0")));
    RateRepository repository = new InMemoryRateRepository(configuredRates);
    configuredRates.clear();

    ZoneRate rate = repository.findRate("DOMESTIC");

    assertEquals(new ZoneRate(new BigDecimal("5"), new BigDecimal("0")), rate);
  }
}
