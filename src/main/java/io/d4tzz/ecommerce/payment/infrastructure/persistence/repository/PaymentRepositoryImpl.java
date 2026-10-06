package io.d4tzz.ecommerce.payment.infrastructure.persistence.repository;

import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.payment.domain.Payment;
import io.d4tzz.ecommerce.payment.domain.PaymentId;
import io.d4tzz.ecommerce.payment.domain.PaymentMethod;
import io.d4tzz.ecommerce.payment.domain.PaymentRepository;
import io.d4tzz.ecommerce.payment.infrastructure.persistence.entity.JpaPayment;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentRepositoryImpl implements PaymentRepository {

    private final SpringPaymentRepository springRepo;

    public PaymentRepositoryImpl(SpringPaymentRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public Optional<Payment> find(PaymentId id) {
        return springRepo.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Optional<Payment> findForUpdate(PaymentId id) {
        return springRepo.findForUpdate(id.value()).map(this::toDomain);
    }

    @Override
    public Optional<Payment> findByTransaction(PaymentMethod method, String shopAccount, String transactionNo) {
        return springRepo
            .findByMethodAndShopAccountAndTransactionNo(method, shopAccount != null ? shopAccount.trim() : null, transactionNo.trim())
            .map(this::toDomain);
    }

    @Override
    public Money refundedAmount(PaymentId paymentId) {
        return Money.of(springRepo.sumRefunds(paymentId.value()));
    }

    @Override
    public void saveOrderReference(Payment payment) {
        JpaPayment jpa = springRepo.findById(payment.getId().value()).orElseThrow();
        jpa.setOrderReference(payment.getOrderReference());
        springRepo.save(jpa);
    }

    @Override
    public Payment add(Payment payment) {
        if (payment.getId() != null) {
            throw new IllegalStateException("Payment " + payment.getId() + " is already recorded");
        }
        JpaPayment jpa = new JpaPayment();
        jpa.setOrderReference(payment.getOrderReference());
        jpa.setType(payment.getType());
        jpa.setMethod(payment.getMethod());
        jpa.setShopAccount(payment.getShopAccount());
        jpa.setAmount(payment.getAmount().amount());
        jpa.setTransactionNo(payment.getTransactionNo());
        jpa.setRefundOf(payment.getRefundOf() != null ? payment.getRefundOf().value() : null);
        jpa.setNote(payment.getNote());
        jpa.setCreatedAt(payment.getCreatedAt());
        return toDomain(springRepo.save(jpa));
    }

    private Payment toDomain(JpaPayment jpa) {
        return Payment.restore(
            PaymentId.of(jpa.getId()),
            jpa.getOrderReference(),
            jpa.getType(),
            jpa.getMethod(),
            jpa.getShopAccount(),
            Money.of(jpa.getAmount()),
            jpa.getTransactionNo(),
            jpa.getRefundOf() != null ? PaymentId.of(jpa.getRefundOf()) : null,
            jpa.getNote(),
            jpa.getCreatedAt()
        );
    }
}
