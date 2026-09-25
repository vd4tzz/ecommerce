package io.d4tzz.ecommerce.infrastructure.cart;

import io.d4tzz.ecommerce.application.cart.CartProductDetailProvider;
import io.d4tzz.ecommerce.infrastructure.catalog.product.JpaVariant;
import io.d4tzz.ecommerce.storage.MediaUrlResolver;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

@Repository
interface SpringVariantCartRepository extends JpaRepository<JpaVariant, Long> {

    @Query("SELECT v FROM Variant v JOIN FETCH v.product p WHERE v.id IN :variantIds")
    List<JpaVariant> findAllWithProductByIdIn(@Param("variantIds") Collection<Long> variantIds);
}

@Component
public class CartProductDetailProviderImpl implements CartProductDetailProvider {

    private final SpringVariantCartRepository variantRepo;
    private final MediaUrlResolver mediaUrlResolver;

    public CartProductDetailProviderImpl(SpringVariantCartRepository variantRepo, MediaUrlResolver mediaUrlResolver) {
        this.variantRepo = variantRepo;
        this.mediaUrlResolver = mediaUrlResolver;
    }

    @Override
    public Map<Long, VariantDetail> getVariantDetails(Collection<Long> variantIds) {
        if (variantIds == null || variantIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<JpaVariant> variants = variantRepo.findAllWithProductByIdIn(variantIds);
        Map<Long, VariantDetail> result = new HashMap<>();

        for (JpaVariant v : variants) {
            String mediaUrl = mediaUrlResolver.resolveUrl(v.getMediaId());
            boolean active = v.getProduct() != null && "ACTIVE".equalsIgnoreCase(v.getProduct().getStatus());

            result.put(v.getId(), new VariantDetail(
                v.getId(),
                v.getSku(),
                v.getProduct() != null ? v.getProduct().getName() : "Unknown",
                v.getProduct() != null ? v.getProduct().getSlug() : "",
                mediaUrl,
                v.getPrice(),
                active
            ));
        }

        return result;
    }
}
