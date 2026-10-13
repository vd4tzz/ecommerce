package io.d4tzz.ecommerce.checkout.application;

import static org.junit.jupiter.api.Assertions.*;

import io.d4tzz.ecommerce.checkout.application.command.CheckoutCommand;
import io.d4tzz.ecommerce.order.application.command.PlaceOrderCommand.PaymentMode;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class CheckoutCommandTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private static CheckoutCommand valid() {
        return new CheckoutCommand(
            7L,
            "Jane",
            "0901234567",
            "Ha Noi",
            "Cau Giay",
            "Dich Vong",
            "1 Xuan Thuy",
            "GHN",
            List.of(new CheckoutCommand.Item(1L, 2)),
            PaymentMode.COD,
            null
        );
    }

    private static Set<String> invalidFields(CheckoutCommand command) {
        return VALIDATOR.validate(command)
            .stream()
            .map(v -> v.getPropertyPath().toString())
            .collect(Collectors.toSet());
    }

    @Test
    void aCompleteRequestWithoutANoteIsValid() {
        assertTrue(VALIDATOR.validate(valid()).isEmpty());
    }

    @Test
    void everyRecipientAndAddressFieldIsRequired() {
        var command = new CheckoutCommand(7L, " ", "", "", " ", "", "", "GHN", valid().items(), PaymentMode.COD, null);

        assertEquals(
            Set.of("recipientName", "recipientPhone", "province", "district", "ward", "detailAddress"),
            invalidFields(command)
        );
    }

    @Test
    void theCarrierIsRequired() {
        var command = new CheckoutCommand(
            7L,
            "Jane",
            "0901234567",
            "p",
            "d",
            "w",
            "a",
            " ",
            valid().items(),
            PaymentMode.COD,
            null
        );

        assertEquals(Set.of("carrier"), invalidFields(command));
    }

    @Test
    void thePaymentModeAndTheUserAreRequired() {
        var command = new CheckoutCommand(
            null,
            "Jane",
            "0901234567",
            "p",
            "d",
            "w",
            "a",
            "GHN",
            valid().items(),
            null,
            null
        );

        assertEquals(Set.of("userId", "paymentMode"), invalidFields(command));
    }

    @Test
    void thereMustBeAtLeastOneItem() {
        var command = new CheckoutCommand(
            7L,
            "Jane",
            "0901234567",
            "p",
            "d",
            "w",
            "a",
            "GHN",
            List.of(),
            PaymentMode.COD,
            null
        );

        assertEquals(Set.of("items"), invalidFields(command));
    }

    @Test
    void everyItemNeedsAVariantAndAPositiveQuantity() {
        var command = new CheckoutCommand(
            7L,
            "Jane",
            "0901234567",
            "p",
            "d",
            "w",
            "a",
            "GHN",
            List.of(new CheckoutCommand.Item(null, 1), new CheckoutCommand.Item(2L, 0)),
            PaymentMode.COD,
            null
        );

        Set<String> fields = invalidFields(command);

        assertEquals(Set.of("items[0].variantId", "items[1].quantity"), fields);
    }

    @Test
    void theNoteIsAtMost500Characters() {
        var ok = new CheckoutCommand(
            7L,
            "Jane",
            "0901234567",
            "p",
            "d",
            "w",
            "a",
            "GHN",
            valid().items(),
            PaymentMode.COD,
            "x".repeat(500)
        );
        var tooLong = new CheckoutCommand(
            7L,
            "Jane",
            "0901234567",
            "p",
            "d",
            "w",
            "a",
            "GHN",
            valid().items(),
            PaymentMode.COD,
            "x".repeat(501)
        );

        assertTrue(VALIDATOR.validate(ok).isEmpty());
        assertEquals(Set.of("note"), invalidFields(tooLong));
    }
}
