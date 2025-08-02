package com.web.service;

import com.web.model.*;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ProductService {
    Product saveProduct(Product product);

    Page<Product> getAllProductsPaginationOfCurrentUser(Integer pageNumber, Integer pageSize, String sortBy, String keyword, UserAccount userAccount);

    Page<Product> getAllProductsPaginationOfAdmin(Integer pageNumber, Integer pageSize, String sortBy, String keyword);
    Page<Product> getAllProductsPaginationOfUser(Integer pageNumber, Integer pageSize, String sortBy, String keyword);

    List<TagDTO> getAllTags();
    void deleteProduct(Long id);

    Product getProductById(Long id);
    Product updateProduct(Product new_product);

    List<ProductDTO> getAllProductsForSearch();
    List<ProductDTO> getAllProductsForSearchByRole(String role);
    List<ProductDTO> getAllProductsOfAdminForSearch();
    List<ProductDTO> getAllProductsOfUserForSearch();
    List<ProductDTO> getAllProductsOfCurrentUserForSearch(UserAccount userAccount);
    List<ProductDTO> getAllProductsFavoritesOfCurrentUserForSearch(UserAccount userAccount);
    List<ProductDTO> getAllProductsOfUserIdForSearch(Long userId);

    Page<Product> getAllProducts(Integer pageNumber, Integer pageSize, String sortBy, String keyword);
    Page<Product> getAllProductsByRole(Integer pageNumber, Integer pageSize, String sortBy, String keyword, String role);
    Page<Product> getFavoriteProductsOfCurrentUser(Integer pageNumber, Integer pageSize, String sortBy, String keyword, UserAccount userAccount);
    Page<Product> getAllProductByTag(Integer pageNumber, Integer pageSize, String sortBy, String tagName);

    Product confirmProduct(Long id, String action);

    List<Product> getProductsOfAdminForHomePage();
    List<Product> getProductsOfUserForHomePage();
    List<Product> getMoreProducts(Product product);
    Page<Product> getAllProductsOfUserId(Integer pageNumber, Integer pageSize, Long userId, String sortBy, String keyword);
}
