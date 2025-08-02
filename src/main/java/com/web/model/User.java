package com.web.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    @OneToOne(cascade = CascadeType.ALL, optional = false, fetch = FetchType.EAGER) // Change to EAGER
    @JoinColumn(name = "userId", referencedColumnName = "userId")
    private UserAccount userAccount;

//    private Boolean isEnable;

    private Boolean accountNonLocked;

    private Integer failedAttempt;

    private Date lockTime;

    private String resetToken;

    private String confirmToken;

//    private Boolean confirmed;

}
