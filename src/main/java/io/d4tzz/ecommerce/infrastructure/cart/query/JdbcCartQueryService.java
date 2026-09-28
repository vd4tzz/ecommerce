package io.d4tzz.ecommerce.infrastructure.cart.query;

import io.d4tzz.ecommerce.query.cart.CartItemView;
import io.d4tzz.ecommerce.query.cart.CartQueryService;
import io.d4tzz.ecommerce.query.cart.CartView;
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
    public CartView getCart(Long userId) {
        if (userId == null) {
            return new CartView(null, null, List.of(), 0, BigDecimal.ZERO);
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
            JOIN cart_items ci ON c.id = ci.cart_id
            JOIN variants v ON ci.variant_id = v.id
            JOIN products p ON v.product_id = p.id
            WHERE c.user_id = :userId
            ORDER BY ci.id ASC
            """;

        record CartRow(
            Long cartId,
            Long itemId,
            Long variantId,
            int quantity,
            String sku,
            BigDecimal unitPrice,
            String mediaId,
            String productName,
            String productSlug
        ) {}

        List<CartRow> rows = jdbcClient
            .sql(sql)
            .param("userId", userId)
            .query((rs, rowNum) ->
                new CartRow(
                    rs.getLong("cart_id"),
                    rs.getLong("item_id"),
                    rs.getLong("variant_id"),
                    rs.getInt("quantity"),
                    rs.getString("sku"),
                    rs.getBigDecimal("unit_price"),
                    rs.getString("media_id"),
                    rs.getString("product_name"),
                    rs.getString("product_slug")
                )
            )
            .list();

        if (rows.isEmpty()) {
            return new CartView(null, userId, List.of(), 0, BigDecimal.ZERO);
        }

        Long cartId = rows.get(0).cartId();
        List<CartItemView> items = new ArrayList<>(rows.size());
        BigDecimal subtotal = BigDecimal.ZERO;
        int totalQuantity = 0;

        for (CartRow row : rows) {
            BigDecimal totalPrice = row.unitPrice().multiply(BigDecimal.valueOf(row.quantity()));

            items.add(
                new CartItemView(
                    row.itemId(),
                    row.variantId(),
                    row.sku(),
                    row.productName(),
                    row.productSlug(),
                    mediaUrlResolver.resolveUrl(row.mediaId()),
                    row.unitPrice(),
                    row.quantity(),
                    totalPrice
                )
            );

            subtotal = subtotal.add(totalPrice);
            totalQuantity += row.quantity();
        }

        return new CartView(cartId, userId, items, totalQuantity, subtotal);
    }
}
