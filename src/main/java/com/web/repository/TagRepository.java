package com.web.repository;

import com.web.model.Tag;
import com.web.model.TagDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByName(String name);
    @Query("SELECT new com.web.model.TagDTO(t.id, t.name) FROM Tag t")
    List<TagDTO> getAllTagDTOs();
}
