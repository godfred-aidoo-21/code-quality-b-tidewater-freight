package com.tidewater.freight.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tidewater.freight.rates.InMemoryRateRepository;
import com.tidewater.freight.rates.RateRepository;
import com.tidewater.freight.rates.ZoneRate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ShippingCalculatorTest {
  @ParameterizedTest
  @CsvSource({
      "DOMESTIC, 10, STANDARD, 50.0",
      "DOMESTIC, 10, EXPRESS, 75.0",
      "DOMESTIC, 40, STANDARD, 220.0",
      "DOMESTIC, 40, EXPRESS, 330.0",
      "REGIONAL, 5, STANDARD, 50.0",
      "REGIONAL, 5, EXPRESS, 75.0",
      "REGIONAL, 40, STANDARD, 350.0",
      "REGIONAL, 40, EXPRESS, 525.0",
      "INTERNATIONAL, 10, STANDARD, 175.0",
      "INTERNATIONAL, 10, EXPRESS, 262.5",
      "INTERNATIONAL, 40, STANDARD, 645.0",
      "INTERNATIONAL, 40, EXPRESS, 967.5"
  })
  void calculatesPriceFromZoneRateFeesAndService(
      String zone, double weightKg, String serviceLevel, double expectedCost) {
    ShippingCalculator calculator = new ShippingCalculator(new InMemoryRateRepository());

    double cost = calculator.calculateCost(zone, weightKg, serviceLevel);

    assertEquals(expectedCost, cost, 0.000001);
  }

  @ParameterizedTest
  @CsvSource({"29.9, 149.5", "30, 150.0", "30.1, 170.5"})
  void addsHeavyItemSurchargeOnlyAboveThirtyKg(double weightKg, double expectedCost) {
    ShippingCalculator calculator = new ShippingCalculator(new InMemoryRateRepository());

    double cost = calculator.calculateCost("DOMESTIC", weightKg, "STANDARD");

    assertEquals(expectedCost, cost, 0.000001);
  }

  @Test
  void calculatesPriceUsingAnInjectedRepository() {
    RateRepository repository = mock(RateRepository.class);
    when(repository.findRate("DOMESTIC")).thenReturn(new ZoneRate(7.0, 3.0));
    ShippingCalculator calculator = new ShippingCalculator(repository);

    double cost = calculator.calculateCost("DOMESTIC", 10, "STANDARD");

    assertEquals(73.0, cost, 0.000001);
  }

  @Test
  void calculatesNewZonePriceFromSuppliedRules() {
    RateRepository repository = new InMemoryRateRepository(
        Map.of("NEW_ZONE", new ZoneRate(12.0, 7.0)));
    ShippingCalculator calculator = new ShippingCalculator(repository);

    double cost = calculator.calculateCost("NEW_ZONE", 40, "EXPRESS");

    assertEquals(760.5, cost, 0.000001);
  }

  @ParameterizedTest
  @CsvSource({"STANDARD, 1.25", "EXPRESS, 1.875"})
  void pricesFractionalKilogramsWithoutRoundingUp(String serviceLevel, double expectedCost) {
    ShippingCalculator calculator = new ShippingCalculator(new InMemoryRateRepository());

    double cost = calculator.calculateCost("DOMESTIC", 0.25, serviceLevel);

    assertEquals(expectedCost, cost, 0.000001);
  }

  @Test
  void pricesWeightImmediatelyAboveTheHeavyItemThreshold() {
    ShippingCalculator calculator = new ShippingCalculator(new InMemoryRateRepository());

    double cost = calculator.calculateCost("DOMESTIC", Math.nextUp(30.0), "STANDARD");

    assertEquals(170.0, cost, 0.000001);
  }
}
