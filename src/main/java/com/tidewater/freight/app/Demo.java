package com.tidewater.freight.app;

import com.tidewater.freight.exception.UnknownZoneException;
import com.tidewater.freight.pricing.ShippingCalculator;
import com.tidewater.freight.rates.InMemoryRateRepository;
import com.tidewater.freight.rates.RateRepository;

public class Demo {
  public static void main(String[] args) {
    RateRepository repository = new InMemoryRateRepository();
    ShippingCalculator calculator = new ShippingCalculator(repository);

    printCost(calculator, "DOMESTIC", 10, "STANDARD");
    printCost(calculator, "INTERNATIONAL", 40, "EXPRESS");
    try {
      printCost(calculator, "MOON", 10, "STANDARD");
    } catch (UnknownZoneException e) {
      System.err.println("Shipment rejected: " + e.getMessage());
    }
  }

  private static void printCost(
      ShippingCalculator calculator, String zone, double weightKg, String serviceLevel) {
    double cost = calculator.calculateCost(zone, weightKg, serviceLevel);
    System.out.printf(
        "Result %s %.1fkg %s -> GHS %.2f%n", zone, weightKg, serviceLevel, cost);
  }
}
