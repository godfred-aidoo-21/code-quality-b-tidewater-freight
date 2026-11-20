package com.tidewater.freight.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tidewater.freight.rates.InMemoryRateRepository;
import com.tidewater.freight.rates.RateRepository;
import com.tidewater.freight.rates.ZoneRate;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ShippingCalculatorTest {
  @ParameterizedTest
  @CsvSource({
      "DOMESTIC, 10, STANDARD, 50.00",
      "DOMESTIC, 10, EXPRESS, 75.00",
      "DOMESTIC, 40, STANDARD, 220.00",
      "DOMESTIC, 40, EXPRESS, 330.00",
      "REGIONAL, 5, STANDARD, 50.00",
      "REGIONAL, 5, EXPRESS, 75.00",
      "REGIONAL, 40, STANDARD, 350.00",
      "REGIONAL, 40, EXPRESS, 525.00",
      "INTERNATIONAL, 10, STANDARD, 175.00",
      "INTERNATIONAL, 10, EXPRESS, 262.50",
      "INTERNATIONAL, 40, STANDARD, 645.00",
      "INTERNATIONAL, 40, EXPRESS, 967.50"
  })
  void calculatesPriceFromZoneRateFeesAndService(
      String zone, double weightKg, String serviceLevel, BigDecimal expectedCost) {
    ShippingCalculator calculator = new ShippingCalculator(new InMemoryRateRepository());

    BigDecimal cost = calculator.calculateCost(zone, weightKg, serviceLevel);

    assertEquals(expectedCost, cost);
  }

  @ParameterizedTest
  @CsvSource({"29.9, 149.50", "30, 150.00", "30.1, 170.50"})
  void addsHeavyItemSurchargeOnlyAboveThirtyKg(double weightKg, BigDecimal expectedCost) {
    ShippingCalculator calculator = new ShippingCalculator(new InMemoryRateRepository());

    BigDecimal cost = calculator.calculateCost("DOMESTIC", weightKg, "STANDARD");

    assertEquals(expectedCost, cost);
  }

  @Test
  void calculatesPriceUsingAnInjectedRepository() {
    RateRepository repository = mock(RateRepository.class);
    when(repository.findRate("DOMESTIC")).thenReturn(zoneRate("7.0", "3.0"));
    ShippingCalculator calculator = new ShippingCalculator(repository);

    BigDecimal cost = calculator.calculateCost("DOMESTIC", 10, "STANDARD");

    assertEquals(new BigDecimal("73.00"), cost);
  }

  @Test
  void calculatesNewZonePriceFromSuppliedRules() {
    RateRepository repository = new InMemoryRateRepository(
        Map.of("NEW_ZONE", zoneRate("12.0", "7.0")));
    ShippingCalculator calculator = new ShippingCalculator(repository);

    BigDecimal cost = calculator.calculateCost("NEW_ZONE", 40, "EXPRESS");

    assertEquals(new BigDecimal("760.50"), cost);
  }

  @ParameterizedTest
  @CsvSource({"STANDARD, 1.25", "EXPRESS, 1.88"})
  void pricesFractionalKilogramsWithoutRoundingUp(String serviceLevel, BigDecimal expectedCost) {
    ShippingCalculator calculator = new ShippingCalculator(new InMemoryRateRepository());

    BigDecimal cost = calculator.calculateCost("DOMESTIC", 0.25, serviceLevel);

    assertEquals(expectedCost, cost);
  }

  @Test
  void pricesWeightImmediatelyAboveTheHeavyItemThreshold() {
    ShippingCalculator calculator = new ShippingCalculator(new InMemoryRateRepository());

    BigDecimal cost = calculator.calculateCost("DOMESTIC", Math.nextUp(30.0), "STANDARD");

    assertEquals(new BigDecimal("170.00"), cost);
  }

  @ParameterizedTest
  @CsvSource({
      "0.004, 0.00", "1.004, 1.00", "1.005, 1.01",
      "1.006, 1.01", "1.015, 1.02", "2.675, 2.68"
  })
  void roundsTheFinalAmountToTwoPlacesUsingHalfUp(double weightKg, BigDecimal expectedCost) {
    ShippingCalculator calculator = new ShippingCalculator(new InMemoryRateRepository(
        Map.of("TEST_ZONE", zoneRate("1", "0"))));

    BigDecimal cost = calculator.calculateCost("TEST_ZONE", weightKg, "STANDARD");

    assertEquals(expectedCost, cost);
  }

  @Test
  void keepsSubCentValuesUntilAfterTheExpressMultiplier() {
    ShippingCalculator calculator = new ShippingCalculator(new InMemoryRateRepository(
        Map.of("TEST_ZONE", zoneRate("0.004", "0.004"))));

    BigDecimal cost = calculator.calculateCost("TEST_ZONE", 1, "EXPRESS");

    assertEquals(new BigDecimal("0.01"), cost);
  }

  @Test
  void addsDecimalRateAndFeeExactly() {
    ShippingCalculator calculator = new ShippingCalculator(new InMemoryRateRepository(
        Map.of("TEST_ZONE", zoneRate("0.10", "0.20"))));

    BigDecimal cost = calculator.calculateCost("TEST_ZONE", 3, "STANDARD");

    assertEquals(new BigDecimal("0.50"), cost);
  }

  private ZoneRate zoneRate(String ratePerKg, String handlingFee) {
    return new ZoneRate(new BigDecimal(ratePerKg), new BigDecimal(handlingFee));
  }
}
