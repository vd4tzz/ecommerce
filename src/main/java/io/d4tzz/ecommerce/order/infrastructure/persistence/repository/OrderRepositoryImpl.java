package io.d4tzz.ecommerce.order.infrastructure.persistence.repository;

import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.domain.catalog.product.ProductId;
import io.d4tzz.ecommerce.domain.catalog.product.VariantId;
import io.d4tzz.ecommerce.domain.user.Address;
import io.d4tzz.ecommerce.domain.user.PhoneNumber;
import io.d4tzz.ecommerce.domain.user.UserId;
import io.d4tzz.ecommerce.order.domain.Order;
import io.d4tzz.ecommerce.order.domain.OrderCode;
import io.d4tzz.ecommerce.order.domain.OrderId;
import io.d4tzz.ecommerce.order.domain.OrderItem;
import io.d4tzz.ecommerce.order.domain.OrderItemId;
import io.d4tzz.ecommerce.order.domain.OrderRepository;
import io.d4tzz.ecommerce.order.domain.Recipient;
import io.d4tzz.ecommerce.order.infrastructure.persistence.entity.JpaOrder;
import io.d4tzz.ecommerce.order.infrastructure.persistence.entity.JpaOrderItem;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepositoryImpl implements OrderRepository {

    private final SpringOrderRepository springRepo;

    public OrderRepositoryImpl(SpringOrderRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public Optional<Order> find(OrderId id) {
        return springRepo.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Optional<Order> findForUpdate(OrderId id) {
        return springRepo.findForUpdate(id.value()).map(this::toDomain);
    }

    @Override
    public Optional<Order> findByOrderCode(OrderCode orderCode) {
        return springRepo.findByOrderCode(orderCode.value()).map(this::toDomain);
    }

    @Override
    public Optional<Order> findForUpdateByOrderCode(OrderCode orderCode) {
        return springRepo.findForUpdateByOrderCode(orderCode.value()).map(this::toDomain);
    }

    @Override
    public List<OrderId> findPaymentExpiredIds(Instant now) {
        return springRepo.findPaymentExpiredIds(now).stream().map(OrderId::of).toList();
    }

    @Override
    public Order save(Order order) {
        JpaOrder jpa;
        if (order.getId() == null) {
            jpa = toNewEntity(order);
        } else {
            jpa = springRepo.findById(order.getId().value()).orElseThrow();
        }

        // Only the lifecycle fields change after creation
        jpa.setStatus(order.getStatus());
        jpa.setPaymentStatus(order.getPaymentStatus());
        jpa.setPaidAt(order.getPaidAt());
        jpa.setPaymentReference(order.getPaymentReference());
        jpa.setCancelReason(order.getCancelReason());
        jpa.setCancelledAt(order.getCancelledAt());
        jpa.setCompletedAt(order.getCompletedAt());
        jpa.setUpdatedAt(order.getUpdatedAt());

        return toDomain(springRepo.save(jpa));
    }

    @Override
    public void delete(OrderId id) {
        springRepo.deleteById(id.value());
    }

    private JpaOrder toNewEntity(Order order) {
        JpaOrder jpa = new JpaOrder();
        jpa.setOrderCode(order.getOrderCode().value());
        jpa.setUserId(order.getUserId().value());
        jpa.setRecipientName(order.getRecipient().fullName());
        jpa.setRecipientPhone(order.getRecipient().phoneNumber().value());

        Address address = order.getShippingAddress();
        jpa.setProvince(address.province());
        jpa.setDistrict(address.district());
        jpa.setWard(address.ward());
        jpa.setAddressDetail(address.detailAddress());

        jpa.setSubtotal(order.getSubtotal().amount());
        jpa.setShippingFee(order.getShippingFee().amount());
        jpa.setTotalAmount(order.getTotalAmount().amount());
        jpa.setNote(order.getNote());
        jpa.setPaymentExpiresAt(order.getPaymentExpiresAt());
        jpa.setCreatedAt(order.getCreatedAt());

        for (OrderItem item : order.getItems()) {
            JpaOrderItem jpaItem = new JpaOrderItem();
            jpaItem.setVariantId(item.getVariantId().value());
            jpaItem.setProductId(item.getProductId().value());
            jpaItem.setProductName(item.getProductName());
            jpaItem.setSku(item.getSku());
            jpaItem.setAttributesDescription(item.getAttributesDescription());
            jpaItem.setThumbnailUrl(item.getThumbnailUrl());
            jpaItem.setUnitPrice(item.getUnitPrice().amount());
            jpaItem.setQuantity(item.getQuantity());
            jpaItem.setTotalPrice(item.getTotalPrice().amount());
            jpa.addItem(jpaItem);
        }
        return jpa;
    }

    private Order toDomain(JpaOrder jpa) {
        List<OrderItem> items = jpa
            .getItems()
            .stream()
            .map(item ->
                OrderItem.restore(
                    OrderItemId.of(item.getId()),
                    VariantId.of(item.getVariantId()),
                    ProductId.of(item.getProductId()),
                    item.getProductName(),
                    item.getSku(),
                    item.getAttributesDescription(),
                    item.getThumbnailUrl(),
                    Money.of(item.getUnitPrice()),
                    item.getQuantity(),
                    Money.of(item.getTotalPrice())
                )
            )
            .toList();

        return Order.restore(
            OrderId.of(jpa.getId()),
            OrderCode.of(jpa.getOrderCode()),
            UserId.of(jpa.getUserId()),
            new Recipient(jpa.getRecipientName(), PhoneNumber.of(jpa.getRecipientPhone())),
            Address.of(jpa.getProvince(), jpa.getDistrict(), jpa.getWard(), jpa.getAddressDetail()),
            items,
            Money.of(jpa.getShippingFee()),
            Money.of(jpa.getTotalAmount()),
            jpa.getStatus(),
            jpa.getPaymentStatus(),
            jpa.getNote(),
            jpa.getPaymentExpiresAt(),
            jpa.getPaidAt(),
            jpa.getPaymentReference(),
            jpa.getCancelReason(),
            jpa.getCancelledAt(),
            jpa.getCompletedAt(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt()
        );
    }
}
