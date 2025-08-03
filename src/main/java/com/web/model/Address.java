package com.web.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String detailAddress;

    private String province;

    private String provinceCode;

    private String ward;

    private String wardCode;

    private String wardFullName;

    private String phone;

    private String fullName;

    private Boolean isDefault;

}
