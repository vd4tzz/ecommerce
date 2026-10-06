package io.d4tzz.ecommerce.payment.infrastructure.persistence.entity;

import io.d4tzz.ecommerce.payment.domain.PaymentMethod;
import io.d4tzz.ecommerce.payment.domain.PaymentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;

/** Insert-only. A duplicate gateway/bank transaction fails on the (method, shop_account, transaction_no) constraint. */
@Entity(name = "Payment")
@Table(
    name = "payments",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_payments_method_account_transaction_no",
            columnNames = { "method", "shop_account", "transaction_no" }
        )
    },
    indexes = {
        @Index(name = "idx_payments_order_reference", columnList = "order_reference"),
        @Index(name = "idx_payments_refund_of", columnList = "refund_of")
    }
)
public class JpaPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    // Updatable only for an unmatched payment getting its order
    @Column(name = "order_reference", length = 50)
    private String orderReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false, length = 20)
    private PaymentType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, updatable = false, length = 20)
    private PaymentMethod method;

    @Column(name = "shop_account", updatable = false, length = 50)
    private String shopAccount;

    @Column(name = "amount", nullable = false, updatable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "transaction_no", updatable = false, length = 100)
    private String transactionNo;

    @Column(name = "refund_of", updatable = false)
    private Long refundOf;

    @Column(name = "note", nullable = false, updatable = false, length = 500)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public JpaPayment() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderReference() {
        return orderReference;
    }

    public void setOrderReference(String orderReference) {
        this.orderReference = orderReference;
    }

    public PaymentType getType() {
        return type;
    }

    public void setType(PaymentType type) {
        this.type = type;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public String getShopAccount() {
        return shopAccount;
    }

    public void setShopAccount(String shopAccount) {
        this.shopAccount = shopAccount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getTransactionNo() {
        return transactionNo;
    }

    public void setTransactionNo(String transactionNo) {
        this.transactionNo = transactionNo;
    }

    public Long getRefundOf() {
        return refundOf;
    }

    public void setRefundOf(Long refundOf) {
        this.refundOf = refundOf;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
