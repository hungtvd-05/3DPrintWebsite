package com.web.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
public class UserAccount {
    @Id
    private Long userId;

    @Column(unique = true, nullable = false)
    private String email;

    private String fullName;

    private String phoneNumber;

    private String address;

    private String province;

    private String provinceCode;

    private String ward;

    private String wardCode;

    private String wardFullName;

    private String profileImage;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String aboutMe;

    private String role;

    private Boolean isEnable;

    private Boolean confirmed;

    @ElementCollection
    @CollectionTable(name = "favorite_products", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "product_id")
    private Set<Long> favoriteProducts = new HashSet<>();

}
