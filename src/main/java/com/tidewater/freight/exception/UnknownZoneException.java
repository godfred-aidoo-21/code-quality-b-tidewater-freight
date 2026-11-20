package com.tidewater.freight.exception;

/** No rate rules are configured for the requested zone. */
public class UnknownZoneException extends ShippingException {
  public UnknownZoneException(String zone) {
    super("Unknown shipping zone: " + zone);
  }
}
