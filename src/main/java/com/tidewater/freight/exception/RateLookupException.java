package com.tidewater.freight.exception;

/** The rate source failed to provide rules; this is distinct from an unknown zone. */
public class RateLookupException extends ShippingException {
  public RateLookupException(String message) {
    super(message);
  }

  public RateLookupException(String message, Throwable cause) {
    super(message, cause);
  }
}
