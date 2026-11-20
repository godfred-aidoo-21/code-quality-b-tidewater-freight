package com.tidewater.freight.app;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

class DemoTest {
  @Test
  @ResourceLock("SYSTEM_OUT")
  @ResourceLock("SYSTEM_ERR")
  @ResourceLock("DEFAULT_LOCALE")
  void displaysPricesAndReportsARejectedShipmentSeparately() {
    PrintStream originalOutput = System.out;
    PrintStream originalError = System.err;
    Locale originalFormatLocale = Locale.getDefault(Locale.Category.FORMAT);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    ByteArrayOutputStream error = new ByteArrayOutputStream();

    try (PrintStream capturedOutput = new PrintStream(output, true, StandardCharsets.UTF_8);
        PrintStream capturedError = new PrintStream(error, true, StandardCharsets.UTF_8)) {
      System.setOut(capturedOutput);
      System.setErr(capturedError);
      Locale.setDefault(Locale.Category.FORMAT, Locale.ROOT);

      Demo.main(new String[0]);
    } finally {
      System.setOut(originalOutput);
      System.setErr(originalError);
      Locale.setDefault(Locale.Category.FORMAT, originalFormatLocale);
    }

    String newline = System.lineSeparator();
    assertAll(
        () -> assertEquals(
            "Result DOMESTIC 10.0kg STANDARD -> GHS 50.00" + newline
                + "Result INTERNATIONAL 40.0kg EXPRESS -> GHS 967.50" + newline,
            output.toString(StandardCharsets.UTF_8)),
        () -> assertEquals(
            "Shipment rejected: Unknown shipping zone: MOON" + newline,
            error.toString(StandardCharsets.UTF_8)));
  }
}
