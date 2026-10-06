package io.d4tzz.ecommerce.shipping.application.port;

import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.domain.user.Address;
import java.util.List;

/** One carrier (GHN, GHTK...). Each carrier is its own adapter; {@link #code()} is how a request picks it. */
public interface ShippingCarrier {
    String code();

    /** The price this carrier asks to send one package. */
    Money priceFor(Address from, Address to, List<PackageItem> items);
}
