package io.d4tzz.ecommerce.infrastructure.cart.query;

import io.d4tzz.ecommerce.query.cart.CartDto;
import io.d4tzz.ecommerce.query.cart.CartItemDto;
import io.d4tzz.ecommerce.query.cart.CartQueryService;
import io.d4tzz.ecommerce.storage.MediaUrlResolver;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class JdbcCartQueryService implements CartQueryService {

    private final JdbcClient jdbcClient;
    private final MediaUrlResolver mediaUrlResolver;

    public JdbcCartQueryService(JdbcClient jdbcClient, MediaUrlResolver mediaUrlResolver) {
        this.jdbcClient = jdbcClient;
        this.mediaUrlResolver = mediaUrlResolver;
    }

    @Override
    public CartDto getCart(Long userId) {
        if (userId == null) {
            return CartDto.empty(null);
        }

        String sql = """
            SELECT
                c.id AS cart_id,
                ci.id AS item_id,
                ci.variant_id,
                ci.quantity,
                v.sku,
                v.price AS unit_price,
                COALESCE(
                    v.media_id,
                    (
                        SELECT pm.media_id
                        FROM product_media pm
                        WHERE pm.product_id = p.id
                        ORDER BY pm.position ASC
                        LIMIT 1
                    )
                ) AS media_id,
                p.name AS product_name,
                p.slug AS product_slug
            FROM carts c
            LEFT JOIN cart_items ci ON c.id = ci.cart_id
            LEFT JOIN variants v ON ci.variant_id = v.id
            LEFT JOIN products p ON v.product_id = p.id
            WHERE c.user_id = :userId
            ORDER BY ci.id ASC
            """;

        record CartRow(
            Long cartId,
            Long itemId,
            Long variantId,
            Integer quantity,
            String sku,
            BigDecimal unitPrice,
            String mediaId,
            String productName,
            String productSlug
        ) {}

        List<CartRow> rows = jdbcClient
            .sql(sql)
            .param("userId", userId)
            .query((rs, rowNum) -> new CartRow(
                rs.getLong("cart_id"),
                rs.getObject("item_id", Long.class),
                rs.getObject("variant_id", Long.class),
                rs.getObject("quantity", Integer.class),
                rs.getString("sku"),
                rs.getBigDecimal("unit_price"),
                rs.getString("media_id"),
                rs.getString("product_name"),
                rs.getString("product_slug")
            ))
            .list();

        if (rows.isEmpty()) {
            return CartDto.empty(userId);
        }

        Long cartId = rows.get(0).cartId();
        List<CartItemDto> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        int totalQuantity = 0;

        for (CartRow row : rows) {
            if (row.itemId() == null) {
                continue;
            }

            BigDecimal unitPrice = row.unitPrice() != null ? row.unitPrice() : BigDecimal.ZERO;
            int quantity = row.quantity() != null ? row.quantity() : 0;
            BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
            String mediaUrl = mediaUrlResolver.resolveUrl(row.mediaId());

            items.add(new CartItemDto(
                row.itemId(),
                row.variantId(),
                row.sku() != null ? row.sku() : "UNKNOWN",
                row.productName() != null ? row.productName() : "Unknown Product",
                row.productSlug() != null ? row.productSlug() : "",
                mediaUrl,
                unitPrice,
                quantity,
                totalPrice
            ));

            subtotal = subtotal.add(totalPrice);
            totalQuantity += quantity;
        }

        return new CartDto(cartId, userId, items, totalQuantity, subtotal);
    }
}
