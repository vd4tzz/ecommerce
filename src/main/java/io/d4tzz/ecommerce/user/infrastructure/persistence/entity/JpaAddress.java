package io.d4tzz.ecommerce.user.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class JpaAddress {

    @Column(name = "address_province")
    private String province;

    @Column(name = "address_district")
    private String district;

    @Column(name = "address_ward")
    private String ward;

    @Column(name = "address_detail")
    private String detailAddress;

    public JpaAddress() {}

    public JpaAddress(String province, String district, String ward, String detailAddress) {
        this.province = province;
        this.district = district;
        this.ward = ward;
        this.detailAddress = detailAddress;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getWard() {
        return ward;
    }

    public void setWard(String ward) {
        this.ward = ward;
    }

    public String getDetailAddress() {
        return detailAddress;
    }

    public void setDetailAddress(String detailAddress) {
        this.detailAddress = detailAddress;
    }
}
