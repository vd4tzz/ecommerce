package io.d4tzz.ecommerce.infrastructure.catalog.option;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity(name = "OptionValue")
@Table(name = "option_values")
public class JpaValue {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id")
    private JpaOption option;

    public JpaValue() {}

    public JpaValue(Long id, String name, JpaOption option) {
        this.id = id;
        this.name = name;
        this.option = option;
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

    public JpaOption getOption() {
        return option;
    }

    public void setOption(JpaOption option) {
        this.option = option;
    }
}
