package com.web.repository;

import com.web.model.Blog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BlogRepository extends JpaRepository<Blog, Long> {
    List<Blog> findByIsBlog(Boolean isBlog);

    @Query(value = "SELECT p FROM Blog p " +
            "WHERE p.isBlog = :isBlog " +
            "ORDER BY " +
            "CASE WHEN :sortBy = 'az' THEN p.title ELSE null END ASC, " +
            "CASE WHEN :sortBy = 'za' THEN p.title ELSE null END DESC, " +
            "CASE WHEN :sortBy = 'old' THEN p.id ELSE null END ASC, " +
            "CASE WHEN :sortBy = '' OR :sortBy = 'new' OR :sortBy IS NULL THEN p.id ELSE null END DESC")
    Page<Blog> getAllBlog(Pageable pageable,
                          @Param("sortBy") String sortBy,
                          @Param("isBlog") Boolean isBlog);

    @Query(value = "SELECT p FROM Blog p " +
            "WHERE p.isBlog = :isBlog " +
            "AND p.title LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY " +
            "CASE WHEN :sortBy = 'az' THEN p.title ELSE null END ASC, " +
            "CASE WHEN :sortBy = 'za' THEN p.title ELSE null END DESC, " +
            "CASE WHEN :sortBy = 'old' THEN p.id ELSE null END ASC, " +
            "CASE WHEN :sortBy = '' OR :sortBy = 'new' OR :sortBy IS NULL THEN p.id ELSE null END DESC")
    Page<Blog> searchAllBlog(Pageable pageable,
                             @Param("keyword") String keyword,
                             @Param("sortBy") String sortBy,
                             @Param("isBlog") Boolean isBlog);

    @Query(value = "SELECT p FROM Blog p " +
            "WHERE p.isBlog = :isBlog " +
            "AND p.status = true " +
            "ORDER BY " +
            "CASE WHEN :sortBy = 'az' THEN p.title ELSE null END ASC, " +
            "CASE WHEN :sortBy = 'za' THEN p.title ELSE null END DESC, " +
            "CASE WHEN :sortBy = 'old' THEN p.id ELSE null END ASC, " +
            "CASE WHEN :sortBy = '' OR :sortBy = 'new' OR :sortBy IS NULL THEN p.id ELSE null END DESC")
    Page<Blog> getAllBlogIsEnable(Pageable pageable,
                          @Param("sortBy") String sortBy,
                          @Param("isBlog") Boolean isBlog);

    @Query(value = "SELECT p FROM Blog p " +
            "WHERE p.isBlog = :isBlog " +
            "AND p.status = true " +
            "AND p.title LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY " +
            "CASE WHEN :sortBy = 'az' THEN p.title ELSE null END ASC, " +
            "CASE WHEN :sortBy = 'za' THEN p.title ELSE null END DESC, " +
            "CASE WHEN :sortBy = 'old' THEN p.id ELSE null END ASC, " +
            "CASE WHEN :sortBy = '' OR :sortBy = 'new' OR :sortBy IS NULL THEN p.id ELSE null END DESC")
    Page<Blog> searchAllBlogIsEnable(Pageable pageable,
                             @Param("keyword") String keyword,
                             @Param("sortBy") String sortBy,
                             @Param("isBlog") Boolean isBlog);

    List<Blog> findByIsBlogAndStatus(Boolean isBlog, Boolean status);
}
