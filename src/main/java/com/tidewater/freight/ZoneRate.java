package com.tidewater.freight;

/** The per-kilogram rate and fixed handling fee for a zone, in GHS. */
public record ZoneRate(double ratePerKg, double handlingFee) {}
