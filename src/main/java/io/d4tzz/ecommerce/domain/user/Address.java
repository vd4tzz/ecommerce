package io.d4tzz.ecommerce.domain.user;

import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;

public record Address(
    String province,
    String district,
    String ward,
    String detailAddress
) {
    public Address {
        if (province == null || province.isBlank()) {
            throw new InvalidArgumentException("Province must not be blank");
        }
        if (district == null || district.isBlank()) {
            throw new InvalidArgumentException("District must not be blank");
        }
        if (ward == null || ward.isBlank()) {
            throw new InvalidArgumentException("Ward must not be blank");
        }
        if (detailAddress == null || detailAddress.isBlank()) {
            throw new InvalidArgumentException("Detail address must not be blank");
        }
        province = province.trim();
        district = district.trim();
        ward = ward.trim();
        detailAddress = detailAddress.trim();
    }

    public static Address of(String province, String district, String ward, String detailAddress) {
        return new Address(province, district, ward, detailAddress);
    }

    public String fullAddress() {
        return String.format("%s, %s, %s, %s", detailAddress, ward, district, province);
    }
}
