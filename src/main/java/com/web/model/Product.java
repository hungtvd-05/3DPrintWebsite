package com.web.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500)
    private String name;

    private Double price = 0.0;

    private Boolean status;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private UserAccount createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    private Boolean isAcceptAdmin = false;

    @ElementCollection
    @CollectionTable(name = "product_image_files",
            joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "file_name")
    private Set<String> imageFiles = new LinkedHashSet<>();

    @ElementCollection
    @CollectionTable(name = "product_stl_files",
            joinColumns = @JoinColumn(name = "product_id"))
    @MapKeyColumn(name = "file_name")
    @AttributeOverrides({
            @AttributeOverride(name = "createdAt", column = @Column(name = "created_at")),
            @AttributeOverride(name = "fileSizeFormatted", column = @Column(name = "file_size_formatted"))
    })
    private Map<String, StlFileInfo> stlFiles = new HashMap<>();

    @ManyToMany
    @JoinTable(
            name = "product_tag",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<Tag> tags = new ArrayList<>();

    private Integer confirmed = 0;

    public List<TagDTO> getTagDTOs() {
        List<TagDTO> tagDTOs = tags.stream()
                .map(tag -> new TagDTO(tag.getId(), tag.getName()))
                .collect(Collectors.toList());
        return tagDTOs;
    }

    public String getInfo() {
        return createdBy.getFullName() + " - " + DateTimeFormatter.ofPattern("dd/MM/yyyy").format(createdAt);
    }
}
