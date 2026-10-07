package io.d4tzz.ecommerce.order.domain;

import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.catalog.domain.product.ProductId;
import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import io.d4tzz.ecommerce.order.domain.event.OrderLine;

public class OrderItem {

    private OrderItemId id;
    private final VariantId variantId;
    private final ProductId productId;
    private final String productName;
    private final String sku;
    private final String optionsDescription;
    private final String thumbnailUrl;
    private final Money unitPrice;
    private final int quantity;
    private final Money totalPrice;

    private OrderItem(
        OrderItemId id,
        VariantId variantId,
        ProductId productId,
        String productName,
        String sku,
        String optionsDescription,
        String thumbnailUrl,
        Money unitPrice,
        int quantity,
        Money totalPrice
    ) {
        if (productName.isBlank()) {
            throw new InvalidArgumentException("Product name must not be blank");
        }
        if (quantity <= 0) {
            throw new InvalidArgumentException("Quantity must be greater than zero");
        }
        if (!totalPrice.equals(unitPrice.multiply(quantity))) {
            throw new InvalidArgumentException("Total price must equal unit price multiplied by quantity");
        }

        this.id = id;
        this.variantId = variantId;
        this.productId = productId;
        this.productName = productName.trim();
        this.sku = sku.trim();
        this.optionsDescription = optionsDescription.trim();
        this.thumbnailUrl = thumbnailUrl;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
    }

    public static OrderItem create(
        VariantId variantId,
        ProductId productId,
        String productName,
        String sku,
        String optionsDescription,
        String thumbnailUrl,
        Money unitPrice,
        int quantity
    ) {
        return new OrderItem(
            null,
            variantId,
            productId,
            productName,
            sku,
            optionsDescription,
            thumbnailUrl,
            unitPrice,
            quantity,
            unitPrice.multiply(quantity)
        );
    }

    public static OrderItem restore(
        OrderItemId id,
        VariantId variantId,
        ProductId productId,
        String productName,
        String sku,
        String optionsDescription,
        String thumbnailUrl,
        Money unitPrice,
        int quantity,
        Money totalPrice
    ) {
        return new OrderItem(
            id,
            variantId,
            productId,
            productName,
            sku,
            optionsDescription,
            thumbnailUrl,
            unitPrice,
            quantity,
            totalPrice
        );
    }

    public OrderLine toLine() {
        return new OrderLine(variantId, quantity);
    }

    public OrderItemId getId() {
        return id;
    }

    public VariantId getVariantId() {
        return variantId;
    }

    public ProductId getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getSku() {
        return sku;
    }

    public String getAttributesDescription() {
        return optionsDescription;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public Money getTotalPrice() {
        return totalPrice;
    }
}
