package com.tidewater.freight.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tidewater.freight.exception.InvalidShipmentException;
import com.tidewater.freight.exception.RateLookupException;
import com.tidewater.freight.exception.UnknownZoneException;
import com.tidewater.freight.rates.RateRepository;
import com.tidewater.freight.rates.ZoneRate;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ShippingFailureTest {
  private final RateRepository repository = mock(RateRepository.class);
  private final ShippingCalculator calculator = new ShippingCalculator(repository);

  @ParameterizedTest
  @ValueSource(doubles = {0, -1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
  void rejectsInvalidWeightBeforeLookingUpRates(double weightKg) {
    InvalidShipmentException failure = assertThrows(InvalidShipmentException.class,
        () -> calculator.calculateCost("DOMESTIC", weightKg, "STANDARD"));

    assertEquals("Weight must be positive and finite, in kilograms", failure.getMessage());
    verifyNoInteractions(repository);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" "})
  void rejectsMissingZoneBeforeLookingUpRates(String zone) {
    InvalidShipmentException failure = assertThrows(InvalidShipmentException.class,
        () -> calculator.calculateCost(zone, 10, "STANDARD"));

    assertEquals("Shipping zone is required", failure.getMessage());
    verifyNoInteractions(repository);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "EXPRES", "express"})
  void rejectsUnsupportedServiceBeforeLookingUpRates(String serviceLevel) {
    InvalidShipmentException failure = assertThrows(InvalidShipmentException.class,
        () -> calculator.calculateCost("DOMESTIC", 10, serviceLevel));

    assertEquals("Service level must be STANDARD or EXPRESS", failure.getMessage());
    verifyNoInteractions(repository);
  }

  @Test
  void addsZoneContextToRateSourceFailureAndPreservesItsCause() {
    IOException sourceFailure = new IOException("Rate file could not be read");
    RateLookupException lookupFailure = new RateLookupException("Rate source failed", sourceFailure);
    when(repository.findRate("DOMESTIC")).thenThrow(lookupFailure);

    RateLookupException failure = assertThrows(RateLookupException.class,
        () -> calculator.calculateCost("DOMESTIC", 10, "STANDARD"));

    assertEquals("Could not retrieve shipping rules for zone DOMESTIC", failure.getMessage());
    assertSame(lookupFailure, failure.getCause());
    assertSame(sourceFailure, failure.getCause().getCause());
  }

  @Test
  void reportsRepositoryContractFailureWhenRulesAreNull() {
    RateLookupException failure = assertThrows(RateLookupException.class,
        () -> calculator.calculateCost("DOMESTIC", 10, "STANDARD"));

    assertEquals("Could not retrieve shipping rules for zone DOMESTIC", failure.getMessage());
    assertEquals("Rate repository returned no rules", failure.getCause().getMessage());
  }

  @Test
  void propagatesUnexpectedRepositoryErrors() {
    IllegalStateException bug = new IllegalStateException("Unexpected adapter bug");
    when(repository.findRate("DOMESTIC")).thenThrow(bug);

    IllegalStateException failure = assertThrows(IllegalStateException.class,
        () -> calculator.calculateCost("DOMESTIC", 10, "STANDARD"));

    assertSame(bug, failure);
  }

  @Test
  void rejectsMissingRepositoryAtConstruction() {
    NullPointerException failure = assertThrows(NullPointerException.class,
        () -> new ShippingCalculator(null));

    assertEquals("Rate repository is required", failure.getMessage());
  }

  @Test
  void rejectsAnOverflowedPrice() {
    when(repository.findRate("DOMESTIC")).thenReturn(new ZoneRate(5, 0));

    ArithmeticException failure = assertThrows(ArithmeticException.class,
        () -> calculator.calculateCost("DOMESTIC", Double.MAX_VALUE, "STANDARD"));

    assertEquals("Shipping cost is not finite for zone DOMESTIC", failure.getMessage());
  }

  @Test
  void rejectsOverflowCausedByTheExpressMultiplier() {
    when(repository.findRate("DOMESTIC")).thenReturn(new ZoneRate(1, 0));

    ArithmeticException failure = assertThrows(ArithmeticException.class,
        () -> calculator.calculateCost("DOMESTIC", Double.MAX_VALUE, "EXPRESS"));

    assertEquals("Shipping cost is not finite for zone DOMESTIC", failure.getMessage());
  }

  @Test
  void letsTheCallerRecognizeAnUnknownZoneFromTheRepository() {
    UnknownZoneException missingZone = new UnknownZoneException("NEW_ZONE");
    when(repository.findRate("NEW_ZONE")).thenThrow(missingZone);

    UnknownZoneException failure = assertThrows(UnknownZoneException.class,
        () -> calculator.calculateCost("NEW_ZONE", 10, "STANDARD"));

    assertSame(missingZone, failure);
  }
}
