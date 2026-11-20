# Tidewater Freight: initial analysis

## What happened when I ran the starter

The starter builds, and all three tests pass. I ran:

```shell
mvn -B -ntp test
mvn -B -ntp exec:java "-Dexec.mainClass=com.tidewater.freight.Demo"
```

The test run reported 3 tests, no failures, no errors and no skipped tests. 

The demo returned:

| Shipment | Cost |
| --- | --- |
| Domestic, 10 kg, standard | GHS 50.00 |
| International, 40 kg, express | GHS 967.50 |
| Unknown zone MOON, 10 kg, standard | GHS 0.00 |

The first two amounts match the formula in the code. The third is the main concern: the service could not price the shipment, but returned an amount anyway.

 Findings are ranked by their effect on pricing, testing and the coming zone change. Line numbers refer to the starter, before refactoring.

## Problems in the code, in priority order

### 1. A failed calculation becomes a zero price

In `ShippingCalculator.java`, lines 34-36 catch any `Exception` and return zero. The repository already throws an exception for an unknown zone, but the calculator hides it. The MOON example demonstrates this.

This breaks **section 6.1: Do not ignore caught exceptions**. A caller receives no indication that anything went wrong. The same catch also hides programming errors, such as calling `equals` on a null service level.

This is the most serious problem. A failed calculation can be mistaken for free shipping, and the original error is lost. The fix should let meaningful failures reach the caller. If a dependency exception needs wrapping, **section 6.3** requires useful context and the original cause.

### 2. Inputs are not checked

`calculateCost()` uses the weight without validation. With domestic standard shipping, a negative weight produces a negative price, zero produces zero, and NaN or infinity can reach the result. A service typo such as `EXPRES` is treated as standard shipping because only `EXPRESS` is checked.

These are correctness problems. **Section 9** also applies because no tests check them. The standard does not define acceptable shipping weights or service spelling, so those rules need agreement before implementation. We should explicitly reject invalid requests rather than price them accidentally.

### 3. The repository cannot be supplied by the caller

At line 6, `ShippingCalculator` creates its own `InMemoryRateRepository`. Although the field uses the `RateRepository` interface, the implementation is still fixed inside the calculator.

This breaks **section 12.5: Dependency inversion principle**. It makes replacing the rate source awkward and prevents a normal unit test from supplying a repository that throws an exception. Constructor injection would make the existing interface useful without requiring a new abstraction.

### 4. Adding a zone means editing the calculator

The repository stores rates, while the calculator selects handling fees through its zone-specific `if/else` chain at lines 14-20. Adding a rate alone is not enough: a new zone can receive a price with its handling fee missing.

This conflicts with **section 12.2: Open-closed principle**, especially given the announced new zone. Knowledge of supported zones is also repeated across two places, creating a consistency problem under **section 2.2**.

Zone rates and fees should be represented together. If the new zone follows the same formula, different rule data may be enough. If its calculation differs, the design needs a way to supply that behavior. We should confirm which is needed and follow the KISS and YAGNI guidance in section 2.2.

### 5. Calculation and console output are mixed

`calculateCost()` retrieves a rate, applies pricing rules, prints a message and decides what the caller sees after a failure. The class also constructs its dependency.

Pricing rules and console presentation have different reasons to change. This breaks **section 12.1: Single-responsibility principle**, **section 4.2: A function should do one thing**, and **section 4.4: A function should have no side effects**. A caller asking for a cost also gets console output it did not request.

The method is roughly 30 lines long, exceeding the usual guidance in **section 4.1**. Its responsibilities matter more than its length. Moving output to `Demo` and dependency construction to the caller would simplify it without splitting every arithmetic step into another class.

### 6. Money has no rounding policy

The service uses `double` for rates and costs. It never defines how to handle a result with more than two decimal places. Fractional weights can also expose floating-point artifacts.

This is a business-contract gap, rather than a direct violation of a rule requiring `BigDecimal`; the supplied standard has no such rule. We need to agree on precision and rounding, then test fractional amounts under **section 9**.

### 7. Pricing values are embedded in the logic

The fees `10` and `25`, weight threshold `30`, surcharge `20` and multiplier `1.5` appear directly in the calculator.

**Section 3.4** requires searchable names instead of magic numbers. Shared policy values need meaningful names, and zone-specific values belong in clearly named rule data. Readers should not need comments to work out which number is a fee and which is a weight limit.

### 8. There are smaller readability issues

The calculator's output line and several lines in `Demo` exceed the 100-character limit in **section 11.1**. Comments such as 'express costs more' repeat what the code does, contrary to **section 10.1**. The domestic branch is empty.

These should be tidied, but they are lower priority than wrong prices, hidden failures and ineffective tests.

## What the AI-written tests actually prove

All three tests are in `ShippingCalculatorTest.java`. Ranked from least useful:

### 1. `calculatesExpressCost()` tests a stub

Lines 18-22 mock the calculator itself, configure it to return `967.5`, and assert that it returns `967.5`. The real calculator never runs. Removing the express multiplier from production code would leave this test green.

The amount happens to match the current formula:

```text
(15 x 40 + 25 + 20) x 1.5 = 967.5
```

But the test never checks that formula. It fails **section 9**, which requires assertions that detect incorrect production behavior. The calculator should be real; its repository can be mocked when needed.

### 2. `returnsACostForAShipment()` has an assertion that cannot check the price

Lines 26-29 call the real calculator, then use `assertNotNull(cost)`. The result is a primitive `double`, boxed for the assertion, so any returned value is non-null. Zero, a negative price and NaN all pass.

Execution could still fail before the assertion, but the assertion proves nothing about the amount. The regional example should return `50.0`, and that is what the test should check. This also fails **section 9**.

### 3. `calculatesACost()` checks too little

Lines 11-14 check that the domestic price is positive. This is weak rather than empty: it would catch zero or a negative result. However, an incorrect result of `1.0` or `500.0` would pass. The expected amount is `50.0`.

Under **section 9**, the test needs to establish the behavior it claims to cover. Its name should also describe the shipment and expected outcome more clearly.

The wildcard static imports at lines 5-6 are a separate style violation of **section 11.1**, which also specifies import grouping and ordering.

## Gaps in the test suite

There are no meaningful checks for handling fees, real express calculations, or the heavy-item boundary. In particular, tests should distinguish weights below 30 kg, exactly 30 kg and above 30 kg. They should also cover fractional amounts, invalid inputs, unknown zones and repository failures. A new-zone example should show that the calculator can use new rules without modification.

**Section 9 requires 100% statement and branch coverage with meaningful assertions.** At this initial diagnosis, coverage and mutation scores had not been measured. The final results are recorded under [How the changes were checked](#how-the-changes-were-checked). PIT is already configured and can help check whether the replacement tests catch changed rates, fees and conditions.

## What matters most before refactoring

The first priority is removing the silent zero-price fallback. The next is replacing the hollow tests so that changes to pricing can be checked with confidence.

The main SOLID problems are that the calculator does several jobs (SRP), needs editing when a new zone has its own fee (OCP), and creates its own repository instead of accepting one from the caller (DIP).

There are useful parts to keep: the repository interface, the small in-memory implementation, and the demo's valid examples. The current formula is a starting point, subject to confirmation of the business rules.



## Decisions

This section records the work completed after the initial diagnosis. The findings above describe the starter and have been kept as they were, including its original file locations and commands.

### Separate pricing, rate lookup and output

`ShippingCalculator` now receives a `RateRepository` through its constructor. `Demo` chooses the in-memory implementation. Tests can supply a different repository without changing the calculator, which addresses DIP (section 12.5).

The calculator handles pricing and validates the details needed for that calculation. It no longer prints anything. `Demo` handles console output and reports an unknown zone as a rejected shipment. This separates presentation from pricing under SRP and sections 4.2 and 4.4. I kept the arithmetic together rather than creating a class for every fee or multiplication.

The files are grouped into `pricing`, `rates`, `exception` and `app`, with tests in matching packages. Each public class has its own file, as required by section 11.1. Shared pricing values now have named constants (section 3.4).

### Keep zone rates and fees together

The repository now returns a `ZoneRate` record containing the per-kilogram rate and fixed handling fee. The calculator uses those values directly, so it no longer needs to recognize DOMESTIC, REGIONAL or INTERNATIONAL in an `if/else` chain.

The in-memory repository accepts a map of zone rules and copies it on construction. A caller changing its original map cannot change the repository's prices afterwards.

ZoneRate stores BigDecimal values and validates them when constructed: the per-kilogram rate must be present and positive, and the handling fee must be present and non-negative. BigDecimal cannot represent NaN or infinity. Invalid configuration throws IllegalArgumentException before the rules can reach the repository. Zero handling fees remain valid for domestic shipping. Focused tests check invalid values and valid fractional rates and fees.

A new zone using the same formula can be added to the supplied map without editing `ShippingCalculator`. The test `calculatesNewZonePriceFromSuppliedRules` supplies NEW_ZONE with a rate of 12 and fee of 7. For 40 kg express, it checks `(12 x 40 + 7 + 20) x 1.5 = 760.5`.

This addresses the known extension under OCP (section 12.2), without adding a framework that the current rules do not need (section 2.2). It has a limit: a zone with a different surcharge policy or calculation formula would need a behavior extension. The actual new zone's rules have not been supplied, so support for every possible future formula is not claimed.

### Make failures visible

The catch-all block and zero-price fallback were removed. The exceptions distinguish problems that callers may need to handle differently:

- `InvalidShipmentException`: missing zone, invalid weight or unsupported service.
- `UnknownZoneException`: no rules are configured for the requested zone.
- `RateLookupException`: the rate source failed or returned no rules.
- `ShippingException`: the shared parent for these shipping failures.

The calculator catches only `RateLookupException` thrown by the repository lookup. A null returned rule is checked after the catch and reported directly with zone context; it has no underlying exception to preserve. It adds the requested zone and keeps the original exception as the cause, following sections 6.1 and 6.3. Unknown-zone exceptions pass through unchanged. Unexpected repository errors also propagate; they are not relabeled as invalid requests or hidden behind a price.

The in-memory source has no file or network operation to recover from. The rate-source failure test therefore uses a mock to simulate an adapter failure with an underlying IOException. It verifies the cause chain; it does not claim to test a real file adapter.

A missing repository is rejected at construction. Monetary arithmetic now uses BigDecimal, so the earlier double-overflow guards and their tests were replaced with decimal arithmetic and rounding checks. The later cleanup removed the unrealistic beyond-double-range examples. Invalid non-finite shipment weights are still rejected. In the demo, the expected unknown-zone failure is displayed on standard error and no price is printed for it. Other unexpected failures remain unhandled by that display code.

### What was kept and what was rewritten

| Inherited part | Decision and reason |
| --- | --- |
| Existing rates, fees and calculation order | Kept for valid shipments. Express still multiplies the full subtotal, and the heavy surcharge still applies only above 30 kg. |
| `RateRepository` | Kept the abstraction and changed its result from a bare number to complete zone rules. |
| `InMemoryRateRepository` | Kept the small adapter; replaced the internally populated mutable map with supplied, copied rules and defaults. |
| `ShippingCalculator` | Refactored construction and zone selection; removed output and swallowed failures; added input checks. |
| `Demo` | Kept the three examples, moved presentation here, and made the static-only class non-instantiable. MOON now produces an explicit rejection. |
| Three AI-written tests | Replaced their assertions and setup. The useful shipment examples remain, but tests now exercise a real calculator and assert expected amounts. |
| Maven test and reporting tools | Kept JUnit 5, Mockito, JaCoCo and PIT. Added enforcement through the mutation-check profile. |

### How the changes were checked

The suite has **61 test executions across five test classes**. Parameterized methods account for many of those executions. It checks zone/service prices, heavy and fractional shipments, weights below/at/above the threshold, invalid inputs, monetary rounding, failure propagation and preserved causes. Repository tests check configured values and the defensive copy. The demo test checks both successful output and rejection output, restoring the streams and formatting locale afterwards.

Mockito is used for the repository seam, never to replace the calculator being tested. Interaction checks supplement assertions about prices or exceptions. Six repeated cases were removed after review, while the important behavior checks and measured coverage remained.

The final verification command was:

```shell
mvn clean verify -Pmutation-check
```

| Check | Result |
| --- | --- |
| JUnit | 61 passed; no failures, errors or skipped tests |
| JaCoCo instructions | 305 covered, 0 missed |
| JaCoCo lines | 68 covered, 0 missed |
| JaCoCo branches | 28 covered, 0 missed |
| PIT | 21 mutations generated, 21 killed; no survivors or uncovered mutations |

PIT reports 50/51 covered lines for mutated classes because it counts Demo's unused private constructor. JaCoCo filters that constructor and reports 100%. These tools use different coverage accounting; no production code was excluded to hide a pricing gap. With the current PIT configuration, no mutations were generated for the new ZoneRate constructor. Its validation is checked by direct tests and JaCoCo coverage; the 21-mutation result does not establish mutation coverage of those guards.

The tests give specific checks for a live rule change: `providesTheConfiguredRateAndHandlingFee` checks rate and fee values, `calculatesPriceFromZoneRateFeesAndService` checks totals and express behavior, and `addsHeavyItemSurchargeOnlyAboveThirtyKg` checks the surcharge boundary.

### Extra improvement: fail the build when mutation results regress

PIT was already present in the starter. Running it alone was not the extra improvement. The new `mutation-check` profile binds PIT to Maven's verify phase and sets a 100% mutation threshold. It also fails when no mutations are generated and writes HTML and XML reports. The profile is opt-in so ordinary unit-test runs stay quick.

This protects the part of the project that was most misleading at the start: a green suite with weak assertions. The existing suite catches all generated mutations, so the threshold preserves that result for later changes.

I checked the failure path with:

```shell
mvn verify -Pmutation-check "-DexcludedTestClasses=com.tidewater.freight.pricing.*"
```

When the profile was introduced, all 48 ordinary tests passed, but excluding pricing tests from PIT allowed three mutations to survive. The score fell to 88%, and Maven failed with `Mutation score of 88 is below threshold of 100`. No source or test files were changed for this experiment. The subsequent full clean run passed and restored the final reports.

A 100% mutation score only covers PIT's generated mutations. Future equivalent mutations need investigation, not meaningless assertions or an unexplained threshold reduction. The profile is available for CI, but no hosted CI pipeline has been added.

### Assumptions and remaining limits

The implementation accepts positive, finite fractional weights, with no invented business maximum. Zone lookup is exact and case-sensitive. Services must be exactly STANDARD or EXPRESS; input is not trimmed or normalized. These choices are documented implementation assumptions, not recorded business approval.

Rates, fees, surcharges and results now use BigDecimal. calculateCost returns a BigDecimal with scale 2. The total is rounded once with HALF_UP, after handling fees, the heavy surcharge and the express multiplier. Rates and intermediate subtotals retain their precision. For example, GHS 1.875 becomes GHS 1.88; a positive amount below half a pesewa can round to GHS 0.00, with no minimum charge invented.

Weights still enter the API as positive finite doubles and are converted with BigDecimal.valueOf. The calculation does not round them to whole kilograms, but it cannot recover precision lost before a caller supplied the double. Configured money values are built from decimal strings, never from new BigDecimal(double). HALF_UP is the chosen implementation policy and still needs business confirmation.

The money tests assert exact values and the two-decimal scale, including rounding ties, decimal addition and rounding only after express. The default PIT operators now generate 21 mutations rather than 25 because BigDecimal method calls replace primitive arithmetic. This score does not establish mutation testing of every decimal arithmetic operation; the rounding and arithmetic regression assertions provide additional checks.

### Pre-submission cleanup

The supplied hygiene review prompted an explicit null-rule stub, separation of the null guard from repository exception wrapping, consistent decimal literals and equality checks, and removal of the unrealistic beyond-double-range test. The README and POM now agree on the opt-in mutation command, and the baseline analysis points to the final results above. The current design was retained. Verification passed with 61 tests, full JaCoCo line/instruction/branch coverage and all 21 generated mutations caught.
