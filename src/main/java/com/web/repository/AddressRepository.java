package com.web.repository;

import com.web.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserId(Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.userId = :userId")
    void updateIsDefaultByUserId(Long userId);

    Optional<Address> findByUserIdAndIsDefault(Long userId, Boolean isDefault);

    Address findFirstByUserId(Long userId);
}
