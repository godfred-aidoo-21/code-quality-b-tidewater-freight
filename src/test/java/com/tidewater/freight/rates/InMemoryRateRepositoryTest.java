package com.tidewater.freight.rates;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tidewater.freight.exception.UnknownZoneException;
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
      String zone, double expectedRatePerKg, double expectedHandlingFee) {
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
  void retrievesRulesForANewZone() {
    RateRepository repository = new InMemoryRateRepository(
        Map.of("NEW_ZONE", new ZoneRate(12, 7)));

    ZoneRate rate = repository.findRate("NEW_ZONE");

    assertEquals(new ZoneRate(12, 7), rate);
  }

  @Test
  void keepsItsRulesWhenTheCallerChangesTheOriginalMap() {
    Map<String, ZoneRate> configuredRates = new HashMap<>();
    configuredRates.put("DOMESTIC", new ZoneRate(5, 0));
    RateRepository repository = new InMemoryRateRepository(configuredRates);
    configuredRates.clear();

    ZoneRate rate = repository.findRate("DOMESTIC");

    assertEquals(new ZoneRate(5, 0), rate);
  }

  @Test
  void reportsAnUnknownZoneWhenNoRulesAreConfigured() {
    RateRepository repository = new InMemoryRateRepository(Map.of());

    UnknownZoneException failure = assertThrows(UnknownZoneException.class,
        () -> repository.findRate("DOMESTIC"));

    assertEquals("Unknown shipping zone: DOMESTIC", failure.getMessage());
  }
}
