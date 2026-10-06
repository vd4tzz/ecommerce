package io.d4tzz.ecommerce.payment.domain;

public enum PaymentMethod {
    /** Cash collected by the carrier on delivery. */
    COD,
    /** Bank transfer, including VietQR. */
    BANK_TRANSFER,
    VNPAY,
    /** Paid on SePay's checkout page (card or bank transfer); the shop account is the SePay merchant id. */
    SEPAY;

    /** Every method except cash produces a transaction number we can reconcile against. */
    public boolean requiresTransactionNo() {
        return this != COD;
    }

    /** Every method except cash moves money through one of the shop's accounts (bank account, merchant code). */
    public boolean requiresShopAccount() {
        return this != COD;
    }
}
