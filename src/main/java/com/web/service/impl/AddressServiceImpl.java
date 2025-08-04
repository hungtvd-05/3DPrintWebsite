package com.web.service.impl;

import com.web.model.Address;
import com.web.model.CartItemDTO;
import com.web.repository.AddressRepository;
import com.web.service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AddressServiceImpl implements AddressService {

    @Autowired
    private AddressRepository addressRepository;

    @Override
    public List<Address> findByUserId(Long userId) {
        return addressRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public Address addAddress(Address address) {
        if (address.getIsDefault()) {
            addressRepository.updateIsDefaultByUserId(address.getUserId());
        }
        return addressRepository.save(address);
    }

    @Override
    public void deleteAddress(Address address) {
        try {
            addressRepository.delete(address);
        } catch (Exception e) {

        }
    }

    @Override
    @Transactional
    public Boolean updateDefaultAddress(Long addressId) {
        try {
            Optional<Address> addressOptional = addressRepository.findById(addressId);

            if (addressOptional.isPresent()) {
                Address address = addressOptional.get();

                addressRepository.updateIsDefaultByUserId(address.getUserId());

                address.setIsDefault(true);
                addressRepository.save(address);

                return true;
            }

            return false;

        } catch (Exception e) {
            throw new RuntimeException("Failed to update default address", e);
        }
    }

    @Override
    public Address findById(Long addressId) {
        return addressRepository.findById(addressId).orElse(null);
    }

    @Override
    public Address getDefaultAddress(Long userId) {
        return addressRepository.findByUserIdAndIsDefault(userId, true)
                .orElse(addressRepository.findFirstByUserId(userId));
    }

}
