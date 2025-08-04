package com.web.repository;

import com.web.model.Product;
import com.web.model.ProductDTO;
import com.web.model.Tag;
import com.web.model.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query(value = "SELECT p FROM Product p " +
            "WHERE (p.createdBy = :userAccount AND p.isDeleted = false ) " +
            "ORDER BY " +
            "CASE WHEN :sortBy = 'az' THEN p.name ELSE null END ASC, " +
            "CASE WHEN :sortBy = 'za' THEN p.name ELSE null END DESC, " +
            "CASE WHEN :sortBy = 'old' THEN p.id ELSE null END ASC, " +
            "CASE WHEN :sortBy = '' OR :sortBy = 'new' OR :sortBy IS NULL THEN p.id ELSE null END DESC")
    Page<Product> findProductsOfCurrentUser(Pageable pageable,
                                            @Param("userAccount") UserAccount userAccount,
                                            @Param("sortBy") String sortBy);

    @Query(value = "SELECT p FROM Product p " +
            "WHERE (p.createdBy = :userAccount AND p.isDeleted = false) " +
            "AND p.name LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY " +
            "CASE WHEN :sortBy = 'az' THEN p.name ELSE null END ASC, " +
            "CASE WHEN :sortBy = 'za' THEN p.name ELSE null END DESC, " +
            "CASE WHEN :sortBy = 'old' THEN p.id ELSE null END ASC, " +
            "CASE WHEN :sortBy = '' OR :sortBy = 'new' OR :sortBy IS NULL THEN p.id ELSE null END DESC")
    Page<Product> searchProductsOfCurrentUser(Pageable pageable,
                                              @Param("keyword") String keyword,
                                              @Param("userAccount") UserAccount userAccount,
                                              @Param("sortBy") String sortBy);

    @Query("SELECT new com.web.model.ProductDTO(p.id, p.name) FROM Product p WHERE p.confirmed = 1 AND p.status = true AND p.isDeleted = false")
    List<ProductDTO> findAllProductDTOs();

    @Query("SELECT new com.web.model.ProductDTO(p.id, p.name) FROM Product p WHERE p.confirmed = 1 AND p.status = true AND p.createdBy.role = :role AND p.isDeleted = false")
    List<ProductDTO> findAllProductDTOsByRole(@Param("role") String role);

    @Query("SELECT new com.web.model.ProductDTO(p.id, p.name) FROM Product p WHERE (p.createdBy.role != 'ROLE_ADMIN' and p.status = true AND p.isDeleted = false)")
    List<ProductDTO> findAllProductDTOsOfUser();

    @Query("SELECT new com.web.model.ProductDTO(p.id, p.name) FROM Product p WHERE p.createdBy.role = 'ROLE_ADMIN' AND p.isDeleted = false")
    List<ProductDTO> findAllProductDTOsOfAdmin();

    @Query("SELECT new com.web.model.ProductDTO(p.id, p.name) FROM Product p WHERE p.createdBy = :userAccount AND p.isDeleted = false")
    List<ProductDTO> findAllProductDTOsOfCurrentUser(@Param("userAccount") UserAccount userAccount);

    @Query(value = "SELECT p FROM Product p " +
            "WHERE (p.confirmed = 1 AND p.status = true AND p.isDeleted = false)" +
            "ORDER BY " +
            "CASE WHEN :sortBy = 'az' THEN p.name ELSE null END ASC, " +
            "CASE WHEN :sortBy = 'za' THEN p.name ELSE null END DESC, " +
            "CASE WHEN :sortBy = 'old' THEN p.id ELSE null END ASC, " +
            "CASE WHEN :sortBy = '' OR :sortBy = 'new' OR :sortBy IS NULL THEN p.id ELSE null END DESC")
    Page<Product> findAllProduct(Pageable pageable, @Param("sortBy") String sortBy);

    @Query(value = "SELECT p FROM Product p " +
            "WHERE (p.confirmed = 1 AND p.status = true AND p.createdBy.role = :role AND p.isDeleted = false)" +
            "ORDER BY " +
            "CASE WHEN :sortBy = 'az' THEN p.name ELSE null END ASC, " +
            "CASE WHEN :sortBy = 'za' THEN p.name ELSE null END DESC, " +
            "CASE WHEN :sortBy = 'old' THEN p.id ELSE null END ASC, " +
            "CASE WHEN :sortBy = '' OR :sortBy = 'new' OR :sortBy IS NULL THEN p.id ELSE null END DESC")
    Page<Product> findAllProductByRole(Pageable pageable, @Param("sortBy") String sortBy, @Param("role") String role);

    @Query(value = "SELECT p FROM Product p " +
            "WHERE (p.confirmed = 1 AND p.status = true AND p.isDeleted = false)" +
            "AND p.name LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY " +
            "CASE WHEN :sortBy = 'az' THEN p.name ELSE null END ASC, " +
            "CASE WHEN :sortBy = 'za' THEN p.name ELSE null END DESC, " +
            "CASE WHEN :sortBy = 'old' THEN p.id ELSE null END ASC, " +
            "CASE WHEN :sortBy = '' OR :sortBy = 'new' OR :sortBy IS NULL THEN p.id ELSE null END DESC")
    Page<Product> searchAllProduct(Pageable pageable, @Param("sortBy") String sortBy, @Param("keyword") String keyword);

    @Query(value = "SELECT p FROM Product p " +
            "WHERE (p.confirmed = 1 AND p.status = true AND p.createdBy.role = :role AND p.isDeleted = false)" +
            "AND p.name LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY " +
            "CASE WHEN :sortBy = 'az' THEN p.name ELSE null END ASC, " +
            "CASE WHEN :sortBy = 'za' THEN p.name ELSE null END DESC, " +
            "CASE WHEN :sortBy = 'old' THEN p.id ELSE null END ASC, " +
            "CASE WHEN :sortBy = '' OR :sortBy = 'new' OR :sortBy IS NULL THEN p.id ELSE null END DESC")
    Page<Product> searchAllProductByRole(Pageable pageable, @Param("sortBy") String sortBy, @Param("keyword") String keyword, @Param("role") String role);

    @Query("SELECT p FROM Product p WHERE p.id IN :favoriteIds AND p.status = true AND p.confirmed = 1 AND p.isDeleted = false " +
            "ORDER BY " +
            "CASE WHEN :sortBy = 'az' THEN p.name END ASC, " +
            "CASE WHEN :sortBy = 'za' THEN p.name END DESC, " +
            "CASE WHEN :sortBy = 'new' THEN p.createdAt END DESC, " +
            "CASE WHEN :sortBy = 'old' THEN p.createdAt END ASC, " +
            "p.id DESC")
    Page<Product> findFavoriteProducts(Pageable pageable, Set<Long> favoriteIds, String sortBy);

    @Query("SELECT p FROM Product p WHERE p.id IN :favoriteIds AND p.status = true AND p.confirmed = 1 AND p.isDeleted = false " +
            "AND LOWER(p.name) LIKE %:keyword% " +
            "ORDER BY " +
            "CASE WHEN :sortBy = 'az' THEN p.name END ASC, " +
            "CASE WHEN :sortBy = 'za' THEN p.name END DESC, " +
            "CASE WHEN :sortBy = 'new' THEN p.createdAt END DESC, " +
            "CASE WHEN :sortBy = 'old' THEN p.createdAt END ASC, " +
            "p.id DESC")
    Page<Product> searchFavoriteProducts(Pageable pageable, Set<Long> favoriteIds, String keyword, String sortBy);

    @Query("SELECT new com.web.model.ProductDTO(p.id, p.name) FROM Product p WHERE p.id IN :favoriteIds AND p.status = true AND p.confirmed = 1 AND p.isDeleted = false")
    List<ProductDTO> findAllProductDTOsForFavorites(Set<Long> favoriteIds);

    @Query("SELECT p FROM Product p JOIN p.tags t WHERE t IN :tags AND (:id IS NULL OR p.id <> :id) " +
            "AND (p.confirmed = 1 AND p.status = true AND p.isDeleted = false)" +
            "ORDER BY " +
            "CASE WHEN :sortBy = 'az' THEN COALESCE(p.name, '') ELSE null END ASC, " +
            "CASE WHEN :sortBy = 'za' THEN COALESCE(p.name, '') ELSE null END DESC, " +
            "CASE WHEN :sortBy = 'old' THEN p.id ELSE null END ASC, " +
            "CASE WHEN :sortBy = '' OR :sortBy = 'new' OR :sortBy IS NULL THEN p.id ELSE null END DESC")
    Page<Product> findByTagsInProductView(Pageable pageable,
                                          @Param("tags") List<Tag> tags,
                                          @Param("sortBy") String sortBy,
                                          @Param("id") Long id);

    @Query("SELECT p FROM Product p WHERE :id IS NULL OR p.id <> :id")
    Page<Product> findByIdNot(@Param("id") Long id, Pageable pageable);

    @Query(value = "SELECT p FROM Product p " +
            "WHERE (p.createdBy.role != 'ROLE_ADMIN' AND p.status = true AND p.isDeleted = false) " +
            "ORDER BY " +
            "CASE WHEN :sortBy = 'az' THEN p.name ELSE null END ASC, " +
            "CASE WHEN :sortBy = 'za' THEN p.name ELSE null END DESC, " +
            "CASE WHEN :sortBy = 'old' THEN p.id ELSE null END ASC, " +
            "CASE WHEN :sortBy = '' OR :sortBy = 'new' OR :sortBy IS NULL THEN p.id ELSE null END DESC")
    Page<Product> findProductsOfUser(Pageable pageable,
                                            @Param("sortBy") String sortBy);

    @Query(value = "SELECT p FROM Product p " +
            "WHERE (p.createdBy.role != 'ROLE_ADMIN' AND p.status = true AND p.isDeleted = false) " +
            "AND p.name LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY " +
            "CASE WHEN :sortBy = 'az' THEN p.name ELSE null END ASC, " +
            "CASE WHEN :sortBy = 'za' THEN p.name ELSE null END DESC, " +
            "CASE WHEN :sortBy = 'old' THEN p.id ELSE null END ASC, " +
            "CASE WHEN :sortBy = '' OR :sortBy = 'new' OR :sortBy IS NULL THEN p.id ELSE null END DESC")
    Page<Product> searchProductsOfUser(Pageable pageable,
                                        @Param("keyword") String keyword,
                                        @Param("sortBy") String sortBy);

    @Query(value = "SELECT p FROM Product p " +
            "WHERE (p.createdBy.role = 'ROLE_ADMIN' AND p.isDeleted = false) " +
            "ORDER BY " +
            "CASE WHEN :sortBy = 'az' THEN p.name ELSE null END ASC, " +
            "CASE WHEN :sortBy = 'za' THEN p.name ELSE null END DESC, " +
            "CASE WHEN :sortBy = 'old' THEN p.id ELSE null END ASC, " +
            "CASE WHEN :sortBy = '' OR :sortBy = 'new' OR :sortBy IS NULL THEN p.id ELSE null END DESC")
    Page<Product> findProductsOfAdmin(Pageable pageable,
                                     @Param("sortBy") String sortBy);

    @Query(value = "SELECT p FROM Product p " +
            "WHERE (p.createdBy.role = 'ROLE_ADMIN' AND p.isDeleted = false) " +
            "AND p.name LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY " +
            "CASE WHEN :sortBy = 'az' THEN p.name ELSE null END ASC, " +
            "CASE WHEN :sortBy = 'za' THEN p.name ELSE null END DESC, " +
            "CASE WHEN :sortBy = 'old' THEN p.id ELSE null END ASC, " +
            "CASE WHEN :sortBy = '' OR :sortBy = 'new' OR :sortBy IS NULL THEN p.id ELSE null END DESC")
    Page<Product> searchProductsOfAdmin(Pageable pageable,
                                              @Param("keyword") String keyword,
                                              @Param("sortBy") String sortBy);

    @Query("SELECT p FROM Product p WHERE p.confirmed = :confirmed AND p.status = :status AND p.createdBy.role = :createdByRole AND p.isDeleted = false ORDER BY p.createdAt DESC LIMIT :limit")
    List<Product> getProductForHome(@Param("confirmed") Integer confirmed,
                                    @Param("status") Boolean status,
                                    @Param("createdByRole") String createdByRole,
                                    @Param("limit") Integer limit);

    @Query("SELECT p FROM Product p JOIN p.tags t WHERE t IN :tags AND (:id IS NULL OR p.id <> :id) " +
            "AND (p.confirmed = 1 AND p.status = true AND p.isDeleted = false)" +
            "ORDER BY p.createdAt DESC " +
            "LIMIT 12")
    List<Product> getMoreProducts(@Param("tags") List<Tag> tags,
                                  @Param("id") Long id);

    @Query("SELECT p FROM Product p JOIN p.tags t WHERE t.name = :tagName " +
            "AND p.confirmed = 1 AND p.status = true AND p.isDeleted = false " +
            "ORDER BY " +
            "CASE WHEN :sortBy = 'az' THEN p.name ELSE null END ASC, " +
            "CASE WHEN :sortBy = 'za' THEN p.name ELSE null END DESC, " +
            "CASE WHEN :sortBy = 'old' THEN p.id ELSE null END ASC, " +
            "CASE WHEN :sortBy = '' OR :sortBy = 'new' OR :sortBy IS NULL THEN p.id ELSE null END DESC")
    Page<Product> findProductsByTagId(Pageable pageable,
                                      @Param("tagName") String tagName,
                                      @Param("sortBy") String sortBy);

    @Query("SELECT p FROM Product p " +
            "WHERE p.confirmed = :confirmed AND p.status = :status AND p.createdBy.userId = :createdByUserId AND p.isDeleted = false " +
            "ORDER BY " +
            "CASE WHEN :sortBy = 'az' THEN p.name ELSE null END ASC, " +
            "CASE WHEN :sortBy = 'za' THEN p.name ELSE null END DESC, " +
            "CASE WHEN :sortBy = 'old' THEN p.id ELSE null END ASC, " +
            "CASE WHEN :sortBy = '' OR :sortBy = 'new' OR :sortBy IS NULL THEN p.id ELSE null END DESC")
    Page<Product> getProductsByUserId(Pageable pageable, Integer confirmed, Boolean status, Long createdByUserId, @Param("sortBy") String sortBy);

    @Query("SELECT p FROM Product p " +
            "WHERE p.confirmed = :confirmed AND p.status = :status AND p.createdBy.userId = :createdByUserId AND p.isDeleted = false " +
            "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY " +
            "CASE WHEN :sortBy = 'az' THEN p.name ELSE null END ASC, " +
            "CASE WHEN :sortBy = 'za' THEN p.name ELSE null END DESC, " +
            "CASE WHEN :sortBy = 'old' THEN p.id ELSE null END ASC, " +
            "CASE WHEN :sortBy = '' OR :sortBy = 'new' OR :sortBy IS NULL THEN p.id ELSE null END DESC")
    Page<Product> searchProductsByUserId(Pageable pageable, Integer confirmed, Boolean status, Long createdByUserId, @Param("sortBy") String sortBy, @Param("keyword") String keyword);

    @Query("SELECT new com.web.model.ProductDTO(p.id, p.name) FROM Product p WHERE p.confirmed = :confirmed AND p.status = :status AND p.createdBy.userId = :createdByUserId AND p.isDeleted = false")
    List<ProductDTO> getProductDTOsByUserId(Integer confirmed, Boolean status, Long createdByUserId);

    Optional<Product> findByIdAndIsDeleted(Long id, Boolean isDeleted);
}
