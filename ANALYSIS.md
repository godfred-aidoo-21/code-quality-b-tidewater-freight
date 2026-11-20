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

The references below are to the supplied *AmaliTech Coding Standards: Java Edition (2026 draft)*. Findings are ranked by their effect on pricing, testing and the coming zone change. Line numbers refer to the starter, before refactoring.

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

**Section 9 requires 100% statement and branch coverage with meaningful assertions.** I have not measured coverage or mutation scores yet. PIT is already configured and can help check whether the replacement tests catch changed rates, fees and conditions.

## What matters most before refactoring

The first priority is removing the silent zero-price fallback. The next is replacing the hollow tests so that changes to pricing can be checked with confidence.

The main SOLID problems are that the calculator does several jobs (SRP), needs editing when a new zone has its own fee (OCP), and creates its own repository instead of accepting one from the caller (DIP).

There are useful parts to keep: the repository interface, the small in-memory implementation, and the demo's valid examples. The current formula is a starting point, subject to confirmation of the business rules.


