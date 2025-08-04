package com.web.service;

import com.web.model.Address;
import com.web.model.CartItemDTO;

import java.util.List;

public interface AddressService {
    List<Address> findByUserId(Long userId);

    Address addAddress(Address address);

    void deleteAddress(Address address);

    Boolean updateDefaultAddress(Long addressId);

    Address findById(Long addressId);

    Address getDefaultAddress(Long userId);

}
