package io.d4tzz.ecommerce.infrastructure.catalog.category;

import java.util.ArrayList;
import java.util.List;

public class JpaCategoryAttribute {

    private String code;
    private String label;
    private String type;
    private List<JpaAttributeValue> values = new ArrayList<>();

    public JpaCategoryAttribute() {}

    public JpaCategoryAttribute(
        String code,
        String label,
        String type,
        List<JpaAttributeValue> values
    ) {
        this.code = code;
        this.label = label;
        this.type = type;
        this.values = values != null ? values : new ArrayList<>();
    }

    public static class JpaAttributeValue {

        private String code;
        private String label;

        public JpaAttributeValue() {}

        public JpaAttributeValue(String code, String label) {
            this.code = code;
            this.label = label;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<JpaAttributeValue> getValues() {
        return values;
    }

    public void setValues(List<JpaAttributeValue> values) {
        this.values = values != null ? values : new ArrayList<>();
    }
}
