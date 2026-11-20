package com.tidewater.freight.exception;

/** Shipment details do not meet the calculator's input contract. */
public class InvalidShipmentException extends ShippingException {
  public InvalidShipmentException(String message) {
    super(message);
  }
}
