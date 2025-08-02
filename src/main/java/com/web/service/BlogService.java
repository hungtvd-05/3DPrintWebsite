package com.web.service;

import com.web.model.Blog;
import org.springframework.data.domain.Page;

import java.util.List;

public interface BlogService {
    Blog saveBlog(Blog blog);
    List<Blog> getAllBlogs();
    List<Blog> getAllPolicies();
    List<Blog> getALlBlogIsEnabled();
    List<Blog> getALlPoliciesIsEnabled();
    Blog getBlogById(Long id);
    Boolean deleteBlog(Long id);

    Page<Blog> getBlogsPage(Integer pageNumber, Integer pageSize, String sortBy, String keyword, Boolean isBlog);

    Page<Blog> getBlogsPageIsEnable(Integer pageNumber, Integer pageSize, String sortBy, String keyword, Boolean isBlog);
}
