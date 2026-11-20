package com.tidewater.freight.rates;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ZoneRateTest {
  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"0", "-1"})
  void rejectsMissingOrNonPositiveRate(BigDecimal ratePerKg) {
    IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
        () -> new ZoneRate(ratePerKg, BigDecimal.ZERO));

    assertEquals("Rate per kilogram must be present and positive", failure.getMessage());
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"-1"})
  void rejectsMissingOrNegativeFee(BigDecimal handlingFee) {
    IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
        () -> new ZoneRate(BigDecimal.ONE, handlingFee));

    assertEquals("Handling fee must be present and non-negative", failure.getMessage());
  }

  @ParameterizedTest
  @CsvSource({"0.5, 0.0", "0.005, 0.0025"})
  void preservesFractionalRatesWithZeroOrPositiveFees(
      BigDecimal ratePerKg, BigDecimal handlingFee) {
    ZoneRate rate = new ZoneRate(ratePerKg, handlingFee);

    assertAll(
        () -> assertEquals(ratePerKg, rate.ratePerKg()),
        () -> assertEquals(handlingFee, rate.handlingFee()));
  }
}
