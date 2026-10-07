package io.d4tzz.ecommerce.shipping.application;

import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import io.d4tzz.ecommerce.common.Address;
import io.d4tzz.ecommerce.shipping.application.port.PackageItem;
import io.d4tzz.ecommerce.shipping.application.port.ShippingCarrier;
import io.d4tzz.ecommerce.shipping.application.port.VariantLookup;
import io.d4tzz.ecommerce.shipping.application.port.WarehouseLookup;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * The shipping fee of an order with the carrier the request names. Each warehouse sends its own package, so
 * the fee is the sum of the package prices. Nothing is saved.
 */
@Service
public class ShippingFeeCalculator {

    private final Map<String, ShippingCarrier> carriersByCode;
    private final WarehouseLookup warehouseLookup;
    private final VariantLookup variantLookup;

    public ShippingFeeCalculator(
        List<ShippingCarrier> carriers,
        WarehouseLookup warehouseLookup,
        VariantLookup variantLookup
    ) {
        this.carriersByCode = carriers
            .stream()
            .collect(Collectors.toMap(carrier -> carrier.code().toUpperCase(Locale.ROOT), Function.identity()));
        this.warehouseLookup = warehouseLookup;
        this.variantLookup = variantLookup;
    }

    /** This method can call api from carrier, must not use in database transaction */
    public Money calculateFee(ShippingFeeInfo info) {
        ShippingCarrier carrier = carriersByCode.get(info.carrier().trim().toUpperCase(Locale.ROOT));
        if (carrier == null) {
            throw new InvalidOperationException("Unknown carrier: " + info.carrier());
        }

        // Get addresses of relevent warehouses
        Map<Long, Address> addresses = warehouseLookup.addressesOf(
            info.packages().stream().map(ShippingFeeInfo.Package::warehouseId).toList()
        );

        // Get measurements of all relevant variant
        Map<VariantId, VariantLookup.Measurements> measurements = variantLookup.measurementsOf(
            info
                .packages()
                .stream()
                .flatMap(pkg -> pkg.items().stream()) // flat items in packages
                .map(item -> VariantId.of(item.variantId()))
                .toList()
        );

        Money total = Money.ZERO;

        // Package is partial (or all) items from an order.
        // The number of package depend on the allocator which finding items of an order.
        // In case all items of an order can get from a warehouse, there is only one package.
        for (ShippingFeeInfo.Package pkg : info.packages()) {
            Address from = addresses.get(pkg.warehouseId());
            if (from == null) {
                throw new InvalidOperationException("Warehouse not found with id: " + pkg.warehouseId());
            }
            List<PackageItem> items = pkg
                .items()
                .stream()
                .map(item -> toPackageItem(item, measurements))
                .toList();

            // Call api from carrier service to get fee for package.
            Money fee = carrier.priceFor(from, info.destination(), items);
            total = total.plus(fee);
        }
        return total;
    }

    private static PackageItem toPackageItem(
        ShippingFeeInfo.Item item,
        Map<VariantId, VariantLookup.Measurements> measurements
    ) {
        VariantLookup.Measurements m = measurements.get(VariantId.of(item.variantId()));
        if (m == null) {
            throw new InvalidOperationException("No size or weight known for variant " + item.variantId());
        }
        return new PackageItem(m.weightGrams(), m.lengthCm(), m.widthCm(), m.heightCm(), item.quantity());
    }
}
