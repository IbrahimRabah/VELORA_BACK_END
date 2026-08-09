package com.velora.api.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.velora.api.common.exception.BusinessException;
import com.velora.api.order.domain.FulfillmentStatus;
import com.velora.api.order.domain.PaymentStatus;
import com.velora.api.order.service.OrderStatusMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OrderStatusMachineTest {

    private OrderStatusMachine machine;

    @BeforeEach
    void setUp() {
        machine = new OrderStatusMachine();
    }

    @Nested
    @DisplayName("The happy path")
    class HappyPath {

        @Test
        void walksFromPendingToDelivered() {
            assertThat(machine.canTransition(
                    FulfillmentStatus.PENDING, FulfillmentStatus.CONFIRMED)).isTrue();
            assertThat(machine.canTransition(
                    FulfillmentStatus.CONFIRMED, FulfillmentStatus.PROCESSING)).isTrue();
            assertThat(machine.canTransition(
                    FulfillmentStatus.PROCESSING, FulfillmentStatus.SHIPPED)).isTrue();
            assertThat(machine.canTransition(
                    FulfillmentStatus.SHIPPED, FulfillmentStatus.OUT_FOR_DELIVERY)).isTrue();
            assertThat(machine.canTransition(
                    FulfillmentStatus.OUT_FOR_DELIVERY, FulfillmentStatus.DELIVERED)).isTrue();
        }

        @Test
        @DisplayName("Steps cannot be skipped")
        void refusesSkippingSteps() {
            assertThat(machine.canTransition(
                    FulfillmentStatus.PENDING, FulfillmentStatus.DELIVERED)).isFalse();
            assertThat(machine.canTransition(
                    FulfillmentStatus.PENDING, FulfillmentStatus.SHIPPED)).isFalse();
            assertThat(machine.canTransition(
                    FulfillmentStatus.CONFIRMED, FulfillmentStatus.OUT_FOR_DELIVERY))
                    .isFalse();
        }

        @Test
        void neverMovesBackwards() {
            assertThat(machine.canTransition(
                    FulfillmentStatus.SHIPPED, FulfillmentStatus.PROCESSING)).isFalse();
            assertThat(machine.canTransition(
                    FulfillmentStatus.DELIVERED, FulfillmentStatus.SHIPPED)).isFalse();
        }
    }

    @Nested
    @DisplayName("Cancellation")
    class Cancellation {

        @Test
        @DisplayName("Allowed until the parcel is dispatched")
        void allowedBeforeShipping() {
            assertThat(machine.canTransition(
                    FulfillmentStatus.PENDING, FulfillmentStatus.CANCELLED)).isTrue();
            assertThat(machine.canTransition(
                    FulfillmentStatus.CONFIRMED, FulfillmentStatus.CANCELLED)).isTrue();
            assertThat(machine.canTransition(
                    FulfillmentStatus.PROCESSING, FulfillmentStatus.CANCELLED)).isTrue();
        }

        @Test
        @DisplayName("Impossible once shipped — the parcel is with the courier")
        void refusedAfterShipping() {
            assertThat(machine.canTransition(
                    FulfillmentStatus.SHIPPED, FulfillmentStatus.CANCELLED)).isFalse();
            assertThat(machine.canTransition(
                    FulfillmentStatus.OUT_FOR_DELIVERY, FulfillmentStatus.CANCELLED)).isFalse();
            assertThat(machine.canTransition(
                    FulfillmentStatus.DELIVERED, FulfillmentStatus.CANCELLED)).isFalse();
        }

        @Test
        void aCancelledOrderIsFinal() {
            assertThat(machine.allowedFrom(FulfillmentStatus.CANCELLED)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Cash-on-delivery failure paths")
    class CodFailures {

        @Test
        @DisplayName("A failed attempt can go back out — the normal case, not an edge case")
        void failedDeliveryRetries() {
            assertThat(machine.canTransition(
                    FulfillmentStatus.OUT_FOR_DELIVERY, FulfillmentStatus.DELIVERY_FAILED))
                    .isTrue();
            assertThat(machine.canTransition(
                    FulfillmentStatus.DELIVERY_FAILED, FulfillmentStatus.OUT_FOR_DELIVERY))
                    .isTrue();
        }

        @Test
        @DisplayName("Refusal at the door leads back to the seller")
        void refusalReturnsToSeller() {
            assertThat(machine.canTransition(
                    FulfillmentStatus.OUT_FOR_DELIVERY, FulfillmentStatus.REFUSED_ON_DELIVERY))
                    .isTrue();
            assertThat(machine.canTransition(
                    FulfillmentStatus.REFUSED_ON_DELIVERY, FulfillmentStatus.RETURNED_TO_SELLER))
                    .isTrue();
        }

        @Test
        @DisplayName("A refused parcel was never delivered")
        void refusedCannotBecomeDelivered() {
            assertThat(machine.canTransition(
                    FulfillmentStatus.REFUSED_ON_DELIVERY, FulfillmentStatus.DELIVERED))
                    .isFalse();
        }

        @Test
        void repeatedFailuresCanGiveUp() {
            assertThat(machine.canTransition(
                    FulfillmentStatus.DELIVERY_FAILED, FulfillmentStatus.RETURNED_TO_SELLER))
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("Returns after delivery")
    class Returns {

        @Test
        void deliveredCanBeReturned() {
            assertThat(machine.canTransition(
                    FulfillmentStatus.DELIVERED, FulfillmentStatus.RETURNED)).isTrue();
            assertThat(machine.canTransition(
                    FulfillmentStatus.DELIVERED, FulfillmentStatus.PARTIALLY_RETURNED))
                    .isTrue();
        }

        @Test
        void aPartialReturnCanBecomeFull() {
            assertThat(machine.canTransition(
                    FulfillmentStatus.PARTIALLY_RETURNED, FulfillmentStatus.RETURNED)).isTrue();
        }

        @Test
        @DisplayName("Only a delivered order can be returned")
        void undeliveredCannotBeReturned() {
            assertThat(machine.canTransition(
                    FulfillmentStatus.PROCESSING, FulfillmentStatus.RETURNED)).isFalse();
            assertThat(machine.canTransition(
                    FulfillmentStatus.SHIPPED, FulfillmentStatus.RETURNED)).isFalse();
        }
    }

    @Nested
    @DisplayName("Payment moves independently of fulfilment")
    class Payment {

        @Test
        @DisplayName("A COD order stays PENDING until the courier remits")
        void codStartsPending() {
            assertThat(machine.canTransition(
                    PaymentStatus.PENDING, PaymentStatus.PAID)).isTrue();
        }

        @Test
        void paidCanBeRefunded() {
            assertThat(machine.canTransition(
                    PaymentStatus.PAID, PaymentStatus.PARTIALLY_REFUNDED)).isTrue();
            assertThat(machine.canTransition(
                    PaymentStatus.PAID, PaymentStatus.REFUNDED)).isTrue();
            assertThat(machine.canTransition(
                    PaymentStatus.PARTIALLY_REFUNDED, PaymentStatus.REFUNDED)).isTrue();
        }

        @Test
        @DisplayName("Nothing can be refunded before it is paid")
        void cannotRefundUnpaid() {
            assertThat(machine.canTransition(
                    PaymentStatus.PENDING, PaymentStatus.REFUNDED)).isFalse();
            assertThat(machine.canTransition(
                    PaymentStatus.FAILED, PaymentStatus.REFUNDED)).isFalse();
        }

        @Test
        @DisplayName("A failed payment can be retried — the customer fixes their card")
        void failedPaymentCanRetry() {
            assertThat(machine.canTransition(
                    PaymentStatus.FAILED, PaymentStatus.PENDING)).isTrue();
        }

        @Test
        void refundIsFinal() {
            assertThat(machine.allowedFrom(PaymentStatus.REFUNDED)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Enforcement")
    class Enforcement {

        @Test
        void rejectsAnIllegalMoveWithAHelpfulMessage() {
            assertThatThrownBy(() -> machine.requireTransition(
                    FulfillmentStatus.PENDING, FulfillmentStatus.DELIVERED))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Cannot move from PENDING to DELIVERED")
                    .hasMessageContaining("Allowed");
        }

        @Test
        @DisplayName("Setting the status it already has is rejected, not silently ignored")
        void rejectsNoOpTransitions() {
            assertThatThrownBy(() -> machine.requireTransition(
                    FulfillmentStatus.SHIPPED, FulfillmentStatus.SHIPPED))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already");
        }

        @Test
        void allowsALegalMove() {
            machine.requireTransition(FulfillmentStatus.PENDING, FulfillmentStatus.CONFIRMED);
            machine.requireTransition(PaymentStatus.PENDING, PaymentStatus.PAID);
        }
    }

    @Test
    @DisplayName("Terminal states really are terminal")
    void terminalStatesAreCorrect() {
        assertThat(FulfillmentStatus.DELIVERED.isTerminal()).isTrue();
        assertThat(FulfillmentStatus.CANCELLED.isTerminal()).isTrue();
        assertThat(FulfillmentStatus.PENDING.isTerminal()).isFalse();
    }

    @Test
    @DisplayName("Dispatched means the goods have left")
    void dispatchedIsCorrect() {
        assertThat(FulfillmentStatus.SHIPPED.isDispatched()).isTrue();
        assertThat(FulfillmentStatus.REFUSED_ON_DELIVERY.isDispatched()).isTrue();
        assertThat(FulfillmentStatus.PROCESSING.isDispatched()).isFalse();
        assertThat(FulfillmentStatus.CANCELLED.isDispatched()).isFalse();
    }
}
