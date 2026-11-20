package com.tidewater.freight.rates;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ZoneRateTest {
  @ParameterizedTest
  @ValueSource(doubles = {0, -1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
  void rejectsInvalidRatePerKilogram(double ratePerKg) {
    IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
        () -> new ZoneRate(ratePerKg, 0));

    assertEquals("Rate per kilogram must be positive and finite", failure.getMessage());
  }

  @ParameterizedTest
  @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
  void rejectsInvalidHandlingFee(double handlingFee) {
    IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
        () -> new ZoneRate(5, handlingFee));

    assertEquals("Handling fee must be non-negative and finite", failure.getMessage());
  }

  @ParameterizedTest
  @CsvSource({"0.5, 0.0", "0.5, 0.25"})
  void acceptsFractionalRatesWithZeroOrPositiveFees(double ratePerKg, double handlingFee) {
    ZoneRate rate = new ZoneRate(ratePerKg, handlingFee);

    assertAll(
        () -> assertEquals(ratePerKg, rate.ratePerKg()),
        () -> assertEquals(handlingFee, rate.handlingFee()));
  }
}
