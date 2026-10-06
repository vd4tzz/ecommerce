package io.d4tzz.ecommerce.payment.domain;

import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import java.time.Instant;
import java.util.Objects;

/**
 * One money movement for an order: a PAYMENT received or a REFUND sent back. Immutable once recorded, like a
 * ledger line: a mistake is corrected by a new record, never by editing. The one exception: a payment that could
 * not be matched to an order is given its order once, by a person (see {@link #assignToOrder}).
 *
 * <p>Payments are recorded whatever the order's state, because the money really moved. Whether the order
 * accepts it is the order's decision.
 *
 * <p>Note: every bank uses the same method BANK_TRANSFER, so the method does not tell which bank a transfer
 * came from. That is why each bank account needs its own {@code shopAccount}.
 *
 * <p>How we know two records are the same transfer: by {@code method + shopAccount + transactionNo} together.
 * <ul>
 *   <li>{@code transactionNo} is the number the bank or gateway gives to the transfer.
 *   <li>One bank never gives the same number to two transfers in the same account. But the shop can receive
 *       money in several accounts (for example Vietcombank and MB Bank), and each bank numbers its transfers
 *       on its own, so two banks can give the same number. {@code shopAccount} tells which account it is.
 *   <li>{@code method} tells which kind of system gave the number (bank transfer or VNPay).
 * </ul>
 * The {@code payments} table has a unique constraint on these three columns.
 *
 * <p>Example of what goes wrong with the transaction number alone:
 * <pre>
 * Vietcombank gives number 123456 to customer A's transfer of 280,000 -> saved as a new payment
 * MB Bank     gives number 123456 to customer B's transfer of 150,000 -> "123456 is already saved"
 * </pre>
 * Customer B's money is treated as a repeat of A's transfer and never saved, so B's order stays unpaid even
 * though B really paid. Adding the method does not help, since both are (BANK_TRANSFER, 123456). With the
 * account in the key, (Vietcombank, 123456) and (MB Bank, 123456) are two different transfers and both are
 * saved. Therefore, the key must also include the shop's account at each bank ({@code shopAccount}).
 * <pre>
 * BANK_TRANSFER, vietcombank_account_number, 123456
 * BANK_TRANSFER, mb_account_number,          123456
 * </pre>
 */
public class Payment {

    private final PaymentId id;
    // code of the order this money is for (e.g. ORD261005WPMH3), as given by the customer or provider;
    // null when no order code could be found, until a person assigns the payment to an order
    private String orderReference;
    private final PaymentType type;
    private final PaymentMethod method;
    private final Money amount;
    // the shop's account the money went into (PAYMENT) or out of (REFUND), as identified by the provider
    // (bank account number, merchant code); null for cash
    private final String shopAccount;
    // null when the method has none (cash)
    private final String transactionNo;
    // the refunded PAYMENT; null for a PAYMENT
    private final PaymentId refundOf;
    private final String note;
    private final Instant createdAt;

    private Payment(
        PaymentId id,
        String orderReference,
        PaymentType type,
        PaymentMethod method,
        String shopAccount,
        Money amount,
        String transactionNo,
        PaymentId refundOf,
        String note,
        Instant createdAt
    ) {
        if (amount.isZero()) {
            throw new InvalidArgumentException("Amount must be greater than zero");
        }
        String normalizedTransactionNo = transactionNo == null || transactionNo.isBlank() ? null : transactionNo.trim();
        if (method.requiresTransactionNo() && normalizedTransactionNo == null) {
            throw new InvalidArgumentException("Transaction number is required for method " + method);
        }
        String normalizedShopAccount = shopAccount == null || shopAccount.isBlank() ? null : shopAccount.trim();
        if (method.requiresShopAccount() != (normalizedShopAccount != null)) {
            throw new InvalidArgumentException(
                method.requiresShopAccount()
                    ? "Shop account is required for method " + method
                    : "Method " + method + " does not go through a shop account"
            );
        }
        if ((type == PaymentType.REFUND) != (refundOf != null)) {
            throw new InvalidArgumentException("A refund must reference exactly one payment");
        }
        this.id = id;
        this.orderReference = orderReference == null || orderReference.isBlank()
            ? null
            : orderReference.trim().toUpperCase();
        this.type = type;
        this.method = method;
        this.amount = amount;
        this.shopAccount = normalizedShopAccount;
        this.transactionNo = normalizedTransactionNo;
        this.refundOf = refundOf;
        this.note = note != null ? note.trim() : "";
        this.createdAt = createdAt;
    }

    public static Payment receive(
        String orderReference,
        PaymentMethod method,
        String shopAccount,
        Money amount,
        String transactionNo,
        String note,
        Instant now
    ) {
        return new Payment(
            null,
            orderReference,
            PaymentType.PAYMENT,
            method,
            shopAccount,
            amount,
            transactionNo,
            null,
            note,
            now
        );
    }

    public static Payment restore(
        PaymentId id,
        String orderReference,
        PaymentType type,
        PaymentMethod method,
        String shopAccount,
        Money amount,
        String transactionNo,
        PaymentId refundOf,
        String note,
        Instant createdAt
    ) {
        return new Payment(
            id,
            orderReference,
            type,
            method,
            shopAccount,
            amount,
            transactionNo,
            refundOf,
            note,
            createdAt
        );
    }

    /**
     * Records money sent back for this payment. Partial refunds are allowed, but all refunds together never
     * exceed the amount received.
     *
     * @param alreadyRefunded sum of the earlier refunds of this payment
     */
    public Payment refund(
        PaymentMethod method,
        String shopAccount,
        Money amountToRefund,
        Money alreadyRefunded,
        String transactionNo,
        String note,
        Instant now
    ) {
        if (type != PaymentType.PAYMENT) {
            throw new InvalidOperationException("Only a payment can be refunded");
        }
        // The remaining amount that can still be refunded is this payment's amount minus what was already refunded.
        // Amount to refund larger than that would give back more money than the customer paid, so reject it.
        // Amount to refund can be less than the remaining, it mean partialy refund.
        Money refundable = this.amount.minus(alreadyRefunded);
        if (amountToRefund.compareTo(refundable) > 0) {
            throw new InvalidOperationException(
                "Refund " + amountToRefund + " exceeds the refundable amount " + refundable + " of payment " + id
            );
        }
        return new Payment(
            null,
            orderReference,
            PaymentType.REFUND,
            method,
            shopAccount,
            amountToRefund,
            transactionNo,
            id,
            note,
            now
        );
    }

    /**
     * Gives an unmatched payment its order, e.g. when the customer mistyped the code in the transfer. This is the
     * only change ever made to a recorded payment, and it happens once: a payment that has an order keeps it.
     */
    public void assignToOrder(String orderCode) {
        if (type != PaymentType.PAYMENT) {
            throw new InvalidOperationException("Only a payment can be assigned to an order");
        }
        if (orderReference != null) {
            throw new InvalidOperationException("Payment " + id + " is already for order " + orderReference);
        }
        if (orderCode == null || orderCode.isBlank()) {
            throw new InvalidArgumentException("Order code must not be blank");
        }
        this.orderReference = orderCode.trim().toUpperCase();
    }

    /** Same money movement, e.g. a gateway callback delivered twice. */
    public boolean isSameAs(String orderReference, PaymentType type, Money amount) {
        String normalized = orderReference == null || orderReference.isBlank()
            ? null
            : orderReference.trim().toUpperCase();
        return Objects.equals(this.orderReference, normalized) && this.type == type && this.amount.equals(amount);
    }

    /** Short human-readable summary, e.g. "REFUND #11 of 100000.00 for order ORD261005WPMH3, refunding payment #10". */
    public String describe() {
        String text = type + " #" + id + " of " + amount + " for order " + (orderReference != null ? orderReference : "(none)");
        return refundOf != null ? text + ", refunding payment #" + refundOf : text;
    }

    public PaymentId getId() {
        return id;
    }

    public String getOrderReference() {
        return orderReference;
    }

    public PaymentType getType() {
        return type;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public String getShopAccount() {
        return shopAccount;
    }

    public Money getAmount() {
        return amount;
    }

    public String getTransactionNo() {
        return transactionNo;
    }

    public PaymentId getRefundOf() {
        return refundOf;
    }

    public String getNote() {
        return note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
