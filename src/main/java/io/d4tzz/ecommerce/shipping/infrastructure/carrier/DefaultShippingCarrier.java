package io.d4tzz.ecommerce.shipping.infrastructure.carrier;

import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.common.Address;
import io.d4tzz.ecommerce.shipping.application.port.PackageItem;
import io.d4tzz.ecommerce.shipping.application.port.ShippingCarrier;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Made-up carrier (code {@code FAKE}) used until a real one (GHN, GHTK...) is integrated. A real carrier does
 * all of this on its side; here we copy how carriers usually price a package:
 * <ol>
 *   <li>Real weight: the weight of all units together.
 *   <li>Volumetric weight: the box size turned into a weight, because a big light box takes as much room as a
 *       heavy one. 5000 cm³ count as 1 kg.
 *   <li>Chargeable weight: the larger of the two.
 *   <li>Price: a price for the first 500 g, plus a price for every started 500 g after that. Sending within
 *       the same province is cheaper.
 * </ol>
 */
@Component
public class DefaultShippingCarrier implements ShippingCarrier {

    private static final int STEP_GRAMS = 500;
    private static final int CM3_PER_KG = 5000;

    private record Price(Money firstStep, Money nextStep) {}

    private static final Price SAME_PROVINCE = new Price(Money.of(16_000), Money.of(2_500));
    private static final Price OTHER_PROVINCE = new Price(Money.of(30_000), Money.of(5_000));

    @Override
    public String code() {
        return "FAKE";
    }

    @Override
    public Money priceFor(Address from, Address to, List<PackageItem> items) {
        long chargeableGrams = Math.max(realGrams(items), volumetricGrams(items));
        long steps = Math.ceilDiv(chargeableGrams, STEP_GRAMS);

        Price price = sameProvince(from.province(), to.province()) ? SAME_PROVINCE : OTHER_PROVINCE;
        return price.firstStep().plus(price.nextStep().multiply(steps - 1));
    }

    private static long realGrams(List<PackageItem> items) {
        long grams = 0;
        for (PackageItem item : items) {
            grams += (long) item.weightGrams() * item.quantity();
        }
        return grams;
    }

    /** Rough box: the longest length, the widest width, and all units stacked on top of each other. */
    private static long volumetricGrams(List<PackageItem> items) {
        long length = 0;
        long width = 0;
        long height = 0;
        for (PackageItem item : items) {
            length = Math.max(length, item.lengthCm());
            width = Math.max(width, item.widthCm());
            height += (long) item.heightCm() * item.quantity();
        }
        long cm3 = length * width * height;
        return Math.ceilDiv(cm3 * 1000, CM3_PER_KG);
    }

    /** "Hà Nội", "TP. Hà Nội" and "ha noi" are the same province. */
    private static boolean sameProvince(String a, String b) {
        return normalize(a).equals(normalize(b));
    }

    private static String normalize(String province) {
        String text = Normalizer.normalize(province, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .replace('đ', 'd')
            .replace('Đ', 'D')
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", " ")
            .trim();
        return text.replaceFirst("^(thanh pho|tp|tinh) ", "");
    }
}
