package com.tidewater.freight;

/** A shipment could not be priced. */
public class ShippingException extends RuntimeException {
  public ShippingException(String message) {
    super(message);
  }

  public ShippingException(String message, Throwable cause) {
    super(message, cause);
  }
}
