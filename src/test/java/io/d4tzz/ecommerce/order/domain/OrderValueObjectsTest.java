package io.d4tzz.ecommerce.order.domain;

import static org.junit.jupiter.api.Assertions.*;

import io.d4tzz.ecommerce.catalog.domain.product.ProductId;
import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.user.domain.PhoneNumber;
import org.junit.jupiter.api.Test;

class OrderValueObjectsTest {

    @Test
    void orderCode_isTrimmedAndUppercased() {
        assertEquals("ORD260110ABCDE", OrderCode.of("  ord260110abcde ").value());
    }

    @Test
    void orderCode_rejectsBlank() {
        assertThrows(InvalidArgumentException.class, () -> OrderCode.of("  "));
    }

    @Test
    void orderCode_generateHasPrefixDateAndFiveCharsFromTheAlphabet() {
        String code = OrderCode.generate().value();

        assertTrue(code.matches("ORD\\d{6}[0-9A-HJ-NP-Z]{5}"), code);
    }

    @Test
    void recipient_trimsNameAndRejectsBlank() {
        Recipient recipient = new Recipient("  Jane Doe ", PhoneNumber.of("0901234567"));

        assertEquals("Jane Doe", recipient.fullName());
        assertThrows(InvalidArgumentException.class, () -> new Recipient(" ", PhoneNumber.of("0901234567")));
    }

    @Test
    void orderItem_totalIsUnitPriceTimesQuantity() {
        OrderItem item = OrderFixtures.item(100_000, 3);

        assertEquals(Money.of(300_000), item.getTotalPrice());
        assertEquals(3, item.toLine().quantity());
    }

    @Test
    void orderItem_rejectsZeroQuantity() {
        assertThrows(InvalidArgumentException.class, () -> OrderFixtures.item(100_000, 0));
    }

    @Test
    void orderItem_rejectsBlankProductName() {
        assertThrows(InvalidArgumentException.class, () ->
            OrderItem.create(VariantId.of(1L), ProductId.of(1L), " ", "SKU", "", null, Money.of(1), 1)
        );
    }

    @Test
    void orderItem_restoreRejectsInconsistentTotal() {
        assertThrows(InvalidArgumentException.class, () ->
            OrderItem.restore(
                OrderItemId.of(1L), VariantId.of(1L), ProductId.of(1L), "Shirt", "SKU", "", null, Money.of(100), 2,
                Money.of(1)
            )
        );
    }
}
