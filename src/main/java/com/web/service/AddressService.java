package com.web.service;

import com.web.model.Address;

import java.util.List;

public interface AddressService {
    List<Address> findByUserId(Long userId);
    Address addAddress(Address address);
    Boolean updateDefaultAddress(Long addressId);
    Address findById(Long addressId);
}
