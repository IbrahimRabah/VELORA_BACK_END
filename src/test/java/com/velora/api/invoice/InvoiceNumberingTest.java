package com.velora.api.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import com.velora.api.invoice.domain.InvoiceSequence;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The numbering rules an auditor cares about.
 *
 * <p>These cover the counter itself. The database enforces the rest through
 * {@code uq_invoice_year_seq}, and the row lock in
 * {@code InvoiceSequenceRepository.lockForYear} is what serialises concurrent
 * issuers — neither can be tested meaningfully without a database.
 */
class InvoiceNumberingTest {

    @Nested
    @DisplayName("The counter")
    class Counter {

        @Test
        @DisplayName("Starts at 1, not 0")
        void startsAtOne() {
            InvoiceSequence sequence = new InvoiceSequence(2026);
            assertThat(sequence.next()).isEqualTo(1);
        }

        @Test
        @DisplayName("Advances by exactly one, every time")
        void advancesByOne() {
            InvoiceSequence sequence = new InvoiceSequence(2026);

            List<Integer> issued = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                issued.add(sequence.next());
            }

            assertThat(issued).hasSize(100);
            assertThat(issued.get(0)).isEqualTo(1);
            assertThat(issued.get(99)).isEqualTo(100);

            // The point of the whole design: no gaps.
            for (int i = 1; i < issued.size(); i++) {
                assertThat(issued.get(i) - issued.get(i - 1))
                        .as("a gap between invoice %d and %d", i - 1, i)
                        .isEqualTo(1);
            }
        }

        @Test
        @DisplayName("Never repeats a number")
        void neverRepeats() {
            InvoiceSequence sequence = new InvoiceSequence(2026);

            Set<Integer> issued = new ArrayList<Integer>() {{
                for (int i = 0; i < 500; i++) {
                    add(sequence.next());
                }
            }}.stream().collect(Collectors.toSet());

            assertThat(issued).hasSize(500);
        }

        @Test
        @DisplayName("Each year counts independently")
        void yearsAreIndependent() {
            InvoiceSequence y2026 = new InvoiceSequence(2026);
            InvoiceSequence y2027 = new InvoiceSequence(2027);

            y2026.next();
            y2026.next();
            y2026.next();

            // A new year restarts at 1 — the number carries the year, so
            // VLR-INV-2026-000003 and VLR-INV-2027-000001 never collide.
            assertThat(y2027.next()).isEqualTo(1);
            assertThat(y2026.getLastNumber()).isEqualTo(3);
        }

        @Test
        @DisplayName("Can resume from an existing high-water mark")
        void resumesFromExisting() {
            // A new counter row seeded from the highest invoice already in the
            // table, so a manual insert cannot cause a duplicate.
            InvoiceSequence sequence = new InvoiceSequence(2026);
            sequence.setLastNumber(47);

            assertThat(sequence.next()).isEqualTo(48);
        }
    }

    @Nested
    @DisplayName("The formatted number")
    class Format {

        @Test
        @DisplayName("Zero-padded to six digits and carries the year")
        void isPaddedAndYearScoped() {
            assertThat(format(2026, 1)).isEqualTo("VLR-INV-2026-000001");
            assertThat(format(2026, 42)).isEqualTo("VLR-INV-2026-000042");
            assertThat(format(2026, 999999)).isEqualTo("VLR-INV-2026-999999");
        }

        @Test
        @DisplayName("Sorts chronologically as plain text")
        void sortsAsText() {
            List<String> numbers = List.of(
                    format(2026, 10), format(2026, 2), format(2027, 1), format(2026, 1));

            List<String> sorted = numbers.stream().sorted().toList();

            // Padding is what makes this work: without it "10" sorts before "2".
            assertThat(sorted).containsExactly(
                    "VLR-INV-2026-000001",
                    "VLR-INV-2026-000002",
                    "VLR-INV-2026-000010",
                    "VLR-INV-2027-000001");
        }

        @Test
        @DisplayName("Is nothing like an order number")
        void differsFromOrderNumber() {
            String invoice = format(2026, 1);

            // Order numbers are random on purpose, so nobody can read your volume
            // from them. Invoice numbers are sequential because the law requires it.
            // Two different requirements, two different generators.
            assertThat(invoice).contains("INV");
            assertThat(invoice).doesNotMatch(".*-\\d{6}-\\d{4}$");
        }
    }

    private String format(int year, int sequence) {
        return "VLR-INV-%d-%06d".formatted(year, sequence);
    }
}
