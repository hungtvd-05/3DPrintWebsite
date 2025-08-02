package com.web.service.impl;

import com.web.model.Blog;
import com.web.repository.BlogRepository;
import com.web.service.BlogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BlogServiceImpl implements BlogService {

    @Autowired
    private BlogRepository blogRepository;

    @Override
    public Blog saveBlog(Blog blog) {
        return blogRepository.save(blog);
    }

    @Override
    public List<Blog> getAllBlogs() {
        return blogRepository.findByIsBlog(true);
    }

    @Override
    public List<Blog> getAllPolicies() {
        return blogRepository.findByIsBlog(false);
    }

    @Override
    public List<Blog> getALlBlogIsEnabled() {
        return blogRepository.findByIsBlogAndStatus(true, true);
    }

    @Override
    public List<Blog> getALlPoliciesIsEnabled() {
        return blogRepository.findByIsBlogAndStatus(false, true);
    }

    @Override
    public Blog getBlogById(Long id) {
        return blogRepository.findById(id).orElse(null);
    }

    @Override
    public Boolean deleteBlog(Long id) {
        if (blogRepository.existsById(id)) {
            blogRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Override
    public Page<Blog> getBlogsPage(Integer pageNumber, Integer pageSize, String sortBy, String keyword, Boolean isBlog) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        if (keyword != null && !keyword.isEmpty()) {
            return blogRepository.searchAllBlog(pageable, keyword.toLowerCase(), sortBy, isBlog);
        }
        return blogRepository.getAllBlog(pageable, sortBy, isBlog);
    }

    @Override
    public Page<Blog> getBlogsPageIsEnable(Integer pageNumber, Integer pageSize, String sortBy, String keyword, Boolean isBlog) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        if (keyword != null && !keyword.isEmpty()) {
            return blogRepository.searchAllBlogIsEnable(pageable, keyword.toLowerCase(), sortBy, isBlog);
        }
        return blogRepository.getAllBlogIsEnable(pageable, sortBy, isBlog);
    }
}
