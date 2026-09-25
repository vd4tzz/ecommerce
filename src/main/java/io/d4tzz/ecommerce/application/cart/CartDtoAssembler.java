package io.d4tzz.ecommerce.application.cart;

import io.d4tzz.ecommerce.domain.cart.Cart;
import io.d4tzz.ecommerce.domain.cart.CartItem;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class CartDtoAssembler {

    private final CartProductDetailProvider detailProvider;

    public CartDtoAssembler(CartProductDetailProvider detailProvider) {
        this.detailProvider = detailProvider;
    }

    public CartDto toDto(Cart cart) {
        if (cart.isEmpty()) {
            return CartDto.empty(cart.getUserId().value());
        }

        Set<Long> variantIds = cart.getItems().stream()
            .map(item -> item.getVariantId().value())
            .collect(Collectors.toSet());

        Map<Long, CartProductDetailProvider.VariantDetail> details = detailProvider.getVariantDetails(variantIds);

        List<CartItemDto> itemDtos = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        int totalQuantity = 0;

        for (CartItem item : cart.getItems()) {
            Long vId = item.getVariantId().value();
            CartProductDetailProvider.VariantDetail detail = details.get(vId);

            String sku = detail != null ? detail.sku() : "UNKNOWN";
            String productName = detail != null ? detail.productName() : "Unknown Product";
            String productSlug = detail != null ? detail.productSlug() : "";
            String mediaUrl = detail != null ? detail.mediaUrl() : null;
            BigDecimal unitPrice = detail != null && detail.price() != null ? detail.price() : BigDecimal.ZERO;
            BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

            Long itemId = item.getId() != null ? item.getId().value() : null;
            itemDtos.add(new CartItemDto(
                itemId,
                vId,
                sku,
                productName,
                productSlug,
                mediaUrl,
                unitPrice,
                item.getQuantity(),
                totalPrice
            ));

            subtotal = subtotal.add(totalPrice);
            totalQuantity += item.getQuantity();
        }

        Long cartId = cart.getId() != null ? cart.getId().value() : null;
        Long userId = cart.getUserId().value();

        return new CartDto(cartId, userId, itemDtos, totalQuantity, subtotal);
    }
}
