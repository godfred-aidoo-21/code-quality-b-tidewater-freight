# Tidewater Freight

A small Java service that calculates shipment costs in Ghana cedis (GHS) from a
shipment's weight, destination zone and service level. This repository contains the
refactored service, its JUnit 5 and Mockito tests, and the Code Quality and Testing
lab analysis.

See [ANALYSIS.md](ANALYSIS.md) for the original diagnosis, AmaliTech section
references, design decisions, what was kept or rewritten, and verification evidence.

## Requirements

- JDK 21
- Maven available as `mvn`

Run commands from the repository root in PowerShell or a POSIX shell such as Bash.
Maven downloads dependencies on the first run.

## Run the demo

```powershell
mvn compile
mvn exec:java "-Dexec.mainClass=com.tidewater.freight.app.Demo"
```

The demo prints two prices to standard output and reports the unknown zone to
standard error:

```text
Result DOMESTIC 10.0kg STANDARD -> GHS 50.00
Result INTERNATIONAL 40.0kg EXPRESS -> GHS 967.50
Shipment rejected: Unknown shipping zone: MOON
```

The order of messages from the two streams can vary. A failed calculation throws
an exception; it does not return a fallback price.

## Pricing rules

| Zone | Rate per kg | Handling fee |
| --- | ---: | ---: |
| DOMESTIC | GHS 5.00 | GHS 0.00 |
| REGIONAL | GHS 8.00 | GHS 10.00 |
| INTERNATIONAL | GHS 15.00 | GHS 25.00 |

1. Multiply the zone's rate by the shipment weight and add its handling fee.
2. Add GHS 20.00 when the weight is strictly greater than 30 kg.
3. For EXPRESS, multiply the entire subtotal by 1.5. STANDARD has no multiplier.
4. Round the final total once to two decimal places using `HALF_UP`.

For example, international express at 40 kg costs
`(15 x 40 + 25 + 20) x 1.5 = GHS 967.50`. Exactly 30 kg has no heavy-item surcharge.

Rates, fees and results use `BigDecimal`. Intermediate amounts retain their
precision: GHS 1.875 rounds to GHS 1.88 only at the final step.

## Inputs and failures

Weights must be positive and finite; fractional kilograms are accepted. Zone names
must match configured keys exactly, and services must be `STANDARD` or `EXPRESS`.
The service does not trim whitespace or normalize case.

| Failure | Result |
| --- | --- |
| Missing zone, invalid weight or unsupported service | `InvalidShipmentException` |
| Zone has no configured rules | `UnknownZoneException` |
| Rate source fails or returns no rules | `RateLookupException`, with zone context and any original cause preserved |
| Unexpected repository error | Propagates to the caller |
| Missing repository at construction | `NullPointerException` with a descriptive message |
| Null/non-positive configured rate or null/negative fee | `IllegalArgumentException` when constructing `ZoneRate` |

The three shipping exceptions share the `ShippingException` parent. The demo
handles the expected unknown-zone rejection by displaying it; the calculator leaves
failure handling to its caller. A production caller is responsible for logging or
reporting these failures; the demo reports expected rejections on standard error.

## Using the calculator

```java
import com.tidewater.freight.pricing.ShippingCalculator;
import com.tidewater.freight.rates.InMemoryRateRepository;
import java.math.BigDecimal;

ShippingCalculator calculator =
    new ShippingCalculator(new InMemoryRateRepository());
BigDecimal cost = calculator.calculateCost("REGIONAL", 5, "STANDARD");
// cost is 50.00, with a scale of 2.
```

The repository is injected through the constructor. It returns a `ZoneRate` holding
the rate and handling fee together. Its in-memory implementation accepts a map of
rules and makes an immutable copy.

To add a zone using the same formula, include its rules in the map supplied to
`InMemoryRateRepository`. That map is the complete configuration, so include the
existing zones that should remain available. The calculator needs no changes.
`calculatesNewZonePriceFromSuppliedRules` demonstrates this in the test suite.

## Project structure

```text
.
|-- ANALYSIS.md                                  # Diagnosis, decisions and verification evidence
|-- README.md                                    # Project overview and run commands
|-- pom.xml                                      # Dependencies, build and quality checks
|-- .gitignore                                   # Files excluded from Git
`-- src/
    |-- main/java/com/tidewater/freight/
    |   |-- app/
    |   |   `-- Demo.java                        # Runs examples and displays prices or errors
    |   |-- pricing/
    |   |   `-- ShippingCalculator.java          # Validates shipments and calculates rounded costs
    |   |-- rates/
    |   |   |-- RateRepository.java              # Contract for retrieving zone rules
    |   |   |-- InMemoryRateRepository.java      # Stores and looks up configured rules
    |   |   `-- ZoneRate.java                    # Holds and validates decimal rates and fees
    |   `-- exception/
    |       |-- ShippingException.java           # Parent for shipping failures
    |       |-- InvalidShipmentException.java    # Invalid shipment details
    |       |-- UnknownZoneException.java        # Zone has no configured rules
    |       `-- RateLookupException.java         # Rate-source failure, with cause and context
    `-- test/java/com/tidewater/freight/
        |-- app/
        |   `-- DemoTest.java                    # Checks price and rejection output
        |-- pricing/
        |   |-- ShippingCalculatorTest.java      # Checks prices, boundaries and rounding
        |   `-- ShippingFailureTest.java         # Checks validation and failure propagation
        `-- rates/
            |-- InMemoryRateRepositoryTest.java  # Checks rules and defensive copying
            `-- ZoneRateTest.java                # Checks valid and invalid rates and fees
```

## Tests and verification

Run the unit and demo-output tests:

```powershell
mvn test
```

Run tests and generate the JaCoCo coverage report:

```powershell
mvn clean verify
```

Run the full submission check, including the mutation threshold:

```powershell
mvn clean verify -Pmutation-check
```

| Report | Location |
| --- | --- |
| JUnit results | `target/surefire-reports/` |
| JaCoCo coverage | `target/site/jacoco/index.html` |
| PIT mutation results | `target/pit-reports/index.html` |
| PIT machine-readable results, with the profile | `target/pit-reports/mutations.xml` |

The latest verified run passed all **61 test executions**, with no failures, errors
or skipped tests. JaCoCo reports **100% line, instruction and branch coverage**.
PIT generated **21 mutations, all caught**. Tests cover prices, boundaries, decimal
rounding, invalid inputs, repository failures, new-zone support and visible errors.
Mockito replaces repository dependencies; tests exercise the real calculator.

PIT's 98% line coverage for mutated classes differs from JaCoCo's 100% because PIT
counts Demo's unused private constructor. The current PIT configuration generates
no mutations for the record constructor and does not mutate every `BigDecimal`
operation. Direct tests cover those guards and decimal calculations. A perfect
mutation score is evidence for the generated mutations, not proof against every bug.

## Extra improvement: enforce mutation results

The starter could generate a PIT report without rejecting a weak score. The
`mutation-check` profile binds PIT to Maven's verify phase and fails the build if
the mutation score is below 100% or no mutations are generated. It is opt-in so
ordinary test runs stay quick, and can be used as a CI command.

The failure path was checked when the profile was introduced: excluding pricing
tests from PIT left all 48 ordinary tests passing, but let three of 25 mutations
survive. Maven rejected the 88% score. A subsequent full run passed. The experiment
and command are recorded in [ANALYSIS.md](ANALYSIS.md#decisions).

Generated reports and compiled output belong in `target/` and are excluded from Git.

## Assumptions and limits

The existing rates, shared heavy-item surcharge and express policy were retained.
The following decisions still need business confirmation:

- `HALF_UP` rounding to two decimal places, with no minimum charge. A positive
  amount below GHS 0.005 can round to GHS 0.00; this is distinct from a failed quote.
- Exact, case-sensitive zone and service names, with no input normalization.
- Positive fractional weights with no business maximum imposed.
- The coming zone using the same formula with different rates and handling fees.
  A different surcharge or calculation policy would need a behavior extension.

Weight enters the API as a `double` and is converted with `BigDecimal.valueOf`.
This avoids introducing binary floating-point arithmetic into the money calculation,
but cannot recover precision already lost in the caller's weight value. Construct
configured monetary values from decimal strings and retain the `BigDecimal` result.
 
Mutation-score enforcement is opt-in:
`mvn verify` generates coverage but does not run or enforce PIT. Use
`mvn clean verify -Pmutation-check` for the full quality check.
