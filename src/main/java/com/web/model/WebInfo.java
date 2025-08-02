package com.web.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
public class WebInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String logo = "";

    private String name = "";

    private String description = "";

    private String address = "";

    private String workingHours = "";

    private String phone = "";

    private String email = "";

    private String freeShipping = "";

    @ElementCollection
    @CollectionTable(name = "web_info_attributes",
            joinColumns = @JoinColumn(name = "web_info_id"))
    @MapKeyColumn(name = "attribute_key")
    @Column(name = "attribute_value")
    private Map<String, String> attributes = new HashMap<>();

}
