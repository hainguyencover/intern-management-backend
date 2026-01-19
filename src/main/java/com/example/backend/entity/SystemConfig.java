package com.example.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "system_configs")
public class SystemConfig extends BaseEntity {

    @Column(name = "config_key", unique = true, nullable = false)
    private String configKey;

    @Column(name = "config_value")
    private String configValue;

    @Column(length = 500)
    private String description;
}
