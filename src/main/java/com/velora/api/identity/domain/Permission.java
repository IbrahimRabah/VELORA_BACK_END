package com.velora.api.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "permission")
@Getter
@Setter
@NoArgsConstructor
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** e.g. ViewOrder, AdjustStock, ProcessRefund */
    @Column(name = "code", nullable = false, length = 80)
    private String code;

    @Column(name = "module", nullable = false, length = 40)
    private String module;

    @Column(name = "description", length = 255)
    private String description;
}
