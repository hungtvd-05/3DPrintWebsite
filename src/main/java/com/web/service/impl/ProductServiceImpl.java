package com.web.service.impl;

import com.web.model.*;
import com.web.repository.*;
import com.web.service.CommentService;
import com.web.service.ProductService;
//import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    String path = System.getProperty("user.dir") + File.separator
            + "src" + File.separator + "main" + File.separator + "resources"
            + File.separator + "static" + File.separator;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private UserRepository usertRepository;

    @Autowired
    private CommentService commentService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Override
    @Transactional
    public Product saveProduct(Product product) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        UserAccount currentUser = userAccountRepository.findByEmail(email);

        if (currentUser == null) {
            return null;
        }

        List<Tag> processedTags = new ArrayList<>();
        for (Tag tag : product.getTags()) {
            Tag persistedTag;
            if (tag.getId() != null) {
                persistedTag = tagRepository.findById(tag.getId())
                        .orElseThrow(() -> new RuntimeException("Tag not found with id: " + tag.getId()));
            } else {
                persistedTag = tagRepository.findByName(tag.getName())
                        .orElseGet(() -> {
                            Tag newTag = new Tag();
                            newTag.setName(tag.getName());
                            return tagRepository.save(newTag);
                        });
            }

            if (!persistedTag.getProducts().contains(product)) {
                persistedTag.getProducts().add(product);
            }

            processedTags.add(persistedTag);
        }

        product.setTags(processedTags);

        product.setCreatedBy(currentUser);

        if (currentUser.getRole().equals("ROLE_ADMIN")) {
            product.setConfirmed(1);
        }

        return productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> getAllProductsPaginationOfCurrentUser(Integer pageNumber, Integer pageSize, String sortBy, String keyword, UserAccount userAccount) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        if (keyword != null && !keyword.isEmpty()) {
            return productRepository.searchProductsOfCurrentUser(pageable, keyword.toLowerCase(), userAccount, sortBy);
        }
        return productRepository.findProductsOfCurrentUser(pageable, userAccount, sortBy);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> getAllProductsPaginationOfAdmin(Integer pageNumber, Integer pageSize, String sortBy, String keyword) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        if (keyword != null && !keyword.isEmpty()) {
            return productRepository.searchProductsOfAdmin(pageable, keyword.toLowerCase(), sortBy);
        }
        return productRepository.findProductsOfAdmin(pageable, sortBy);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagDTO> getAllTags() {
        return tagRepository.getAllTagDTOs();
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id).orElse(null);

        if (product != null) {
            commentService.deleteByCreatedOn(product);
            for (String img : product.getImageFiles()) {
                File file = new File(path + "product" + File.separator + "img" + File.separator + img);
                if (file.exists()) {
                    file.delete();
                }
            }
            for (String stl : product.getStlFiles().keySet()) {
                File file = new File(path + "product" + File.separator + "stl" + File.separator + stl);
                if (file.exists()) {
                    file.delete();
                }
                File imgFile = new File(path + "product" + File.separator + "stl" + File.separator + stl + "_preview.png");
                if (imgFile.exists()) {
                    imgFile.delete();
                }
            }
            productRepository.delete(product);
        } else {
            throw new RuntimeException("Product not found with id: " + id);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Product getProductById(Long id) {
        return productRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public Product updateProduct(Product new_product) {

        Product product = productRepository.findById(new_product.getId())
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + new_product.getId()));

        List<Tag> processedTags = new ArrayList<>();
        for (Tag tag : new_product.getTags()) {
            Tag persistedTag;
            if (tag.getId() == null) {
                Tag newTag = new Tag();
                newTag.setName(tag.getName());
                persistedTag = tagRepository.save(newTag);
            } else {
                persistedTag = tag;
            }

            processedTags.add(persistedTag);
        }

        product.setTags(processedTags);
        product.setName(new_product.getName());
        product.setDescription(new_product.getDescription());
        product.setPrice(new_product.getPrice());
        product.setStatus(new_product.getStatus());
        product.setImageFiles(new_product.getImageFiles());
        product.setStlFiles(new_product.getStlFiles());
        product.setCreatedBy(new_product.getCreatedBy());
        product.setConfirmed(new_product.getConfirmed());

        return productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProductsForSearch() {
        return productRepository.findAllProductDTOs();
    }

    @Override
    public List<ProductDTO> getAllProductsForSearchByRole(String role) {
        return productRepository.findAllProductDTOsByRole(role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProductsOfAdminForSearch() {
        return productRepository.findAllProductDTOsOfAdmin();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProductsOfUserForSearch() {
        return productRepository.findAllProductDTOsOfUser();
    }

    @Override
    public List<ProductDTO> getAllProductsOfCurrentUserForSearch(UserAccount userAccount) {
        return productRepository.findAllProductDTOsOfCurrentUser(userAccount);
    }

    @Override
    public List<ProductDTO> getAllProductsFavoritesOfCurrentUserForSearch(UserAccount userAccount) {
        return productRepository.findAllProductDTOsForFavorites(userAccount.getFavoriteProducts());
    }

    @Override
    public List<ProductDTO> getAllProductsOfUserIdForSearch(Long userId) {
        return productRepository.getProductDTOsByUserId(1, true, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> getAllProducts(Integer pageNumber, Integer pageSize, String sortBy, String keyword) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        if (keyword != null && !keyword.isEmpty()) {
            return productRepository.searchAllProduct(pageable, sortBy, keyword.toLowerCase());
        }
        return productRepository.findAllProduct(pageable, sortBy);
    }

    @Override
    public Page<Product> getAllProductsByRole(Integer pageNumber, Integer pageSize, String sortBy, String keyword, String role) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        if (keyword != null && !keyword.isEmpty()) {
            return productRepository.searchAllProductByRole(pageable, sortBy, keyword.toLowerCase(), role);
        }
        return productRepository.findAllProductByRole(pageable, sortBy, role);
    }

    @Override
    public Page<Product> getFavoriteProductsOfCurrentUser(Integer pageNumber, Integer pageSize, String sortBy, String keyword, UserAccount userAccount) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        if (keyword != null && !keyword.isEmpty()) {
            return productRepository.searchFavoriteProducts(pageable, userAccount.getFavoriteProducts(), keyword.toLowerCase(), sortBy);
        }
        return productRepository.findFavoriteProducts(pageable, userAccount.getFavoriteProducts(), sortBy);
    }

    @Override
    public Page<Product> getAllProductByTag(Integer pageNumber, Integer pageSize, String sortBy, String tagName) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        return productRepository.findProductsByTagId(pageable, tagName, sortBy);
    }

    @Override
    public Page<Product> getAllProductsPaginationOfUser(Integer pageNumber, Integer pageSize, String sortBy, String keyword) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        if (keyword != null && !keyword.isEmpty()) {
            return productRepository.searchProductsOfUser(pageable, keyword.toLowerCase(), sortBy);
        }
        return productRepository.findProductsOfUser(pageable, sortBy);
    }

    @Override
    @Transactional
    public Product confirmProduct(Long id, String action) {

        Product product = productRepository.findById(id).orElse(null);

        if (product == null) {
            return null;
        }

        if (action.equals("confirm")) {
            product.setConfirmed(1);
            return productRepository.save(product);
        } else if (action.equals("reject")) {
            product.setConfirmed(-1);
            return productRepository.save(product);
        }

        return null;
    }

    @Override
    public List<Product> getProductsOfAdminForHomePage() {
        return productRepository.getProductForHome(1, true, "ROLE_ADMIN", 12);
    }

    @Override
    public List<Product> getProductsOfUserForHomePage() {
        return productRepository.getProductForHome(1, true, "ROLE_USER", 36);
    }

    @Override
    public List<Product> getMoreProducts(Product product) {
        return productRepository.getMoreProducts(product.getTags(), product.getId());
    }

    @Override
    public Page<Product> getAllProductsOfUserId(Integer pageNumber, Integer pageSize, Long userId, String sortBy, String keyword) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        if (keyword != null && !keyword.isEmpty()) {
            return productRepository.searchProductsByUserId(
                    pageable, 1, true, userId, sortBy, keyword.toLowerCase());
        }
        return productRepository.getProductsByUserId(
                pageable, 1, true, userId, sortBy);
    }


}
