package io.d4tzz.ecommerce.infrastructure.catalog.option;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "option")
@Table(name = "options")
public class JpaOption {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String name;

    @OneToMany(
        mappedBy = "option",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<JpaValue> values = new ArrayList<>();

    public JpaOption() {}

    public JpaOption(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public void addValue(JpaValue value) {
        this.values.add(value);
        value.setOption(this);
    }

    public void removeValue(JpaValue value) {
        this.values.remove(value);
        value.setOption(null);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<JpaValue> getValues() {
        return values;
    }

    public void setValues(List<JpaValue> values) {
        this.values = values;
    }
}
