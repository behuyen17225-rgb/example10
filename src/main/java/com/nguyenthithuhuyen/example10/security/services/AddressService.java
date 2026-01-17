package com.nguyenthithuhuyen.example10.security.services;

import com.nguyenthithuhuyen.example10.entity.Address;
import com.nguyenthithuhuyen.example10.entity.User;
import com.nguyenthithuhuyen.example10.repository.AddressRepository;
import com.nguyenthithuhuyen.example10.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private static final Logger log = LoggerFactory.getLogger(AddressService.class);

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    /* ===== CREATE ===== */
    @Transactional
    public Address createAddress(String username, Address addressRequest) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Address address = new Address();
        address.setUser(user);
        address.setStreetAddress(addressRequest.getStreetAddress());
        address.setWard(addressRequest.getWard());
        address.setDistrict(addressRequest.getDistrict());
        address.setCity(addressRequest.getCity());
        address.setPostalCode(addressRequest.getPostalCode());
        address.setPhone(addressRequest.getPhone());
        address.setRecipientName(addressRequest.getRecipientName());
        address.setCreatedAt(LocalDateTime.now());
        address.setUpdatedAt(LocalDateTime.now());

        // Nếu là địa chỉ đầu tiên, set làm default
        List<Address> existingAddresses = addressRepository.findByUser(user);
        if (existingAddresses.isEmpty()) {
            address.setIsDefault(true);
        } else {
            // Nếu request đánh dấu là default, unset default của các địa chỉ cũ
            if (addressRequest.getIsDefault() != null && addressRequest.getIsDefault()) {
                existingAddresses.stream()
                        .filter(Address::getIsDefault)
                        .forEach(addr -> {
                            addr.setIsDefault(false);
                            addressRepository.save(addr);
                        });
                address.setIsDefault(true);
            }
        }

        Address saved = addressRepository.save(address);
        log.info("✅ Address created for user: {}", username);
        return saved;
    }

    /* ===== READ ===== */
    @Transactional(readOnly = true)
    public List<Address> getAddressesByUsername(String username) {
        return addressRepository.findByUser_Username(username);
    }

    @Transactional(readOnly = true)
    public Address getAddressById(Long id) {
        return addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Address not found"));
    }

    @Transactional(readOnly = true)
    public Address getDefaultAddress(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return addressRepository.findByUserAndIsDefaultTrue(user)
                .orElse(null);
    }

    /* ===== UPDATE ===== */
    @Transactional
    public Address updateAddress(Long id, String username, Address addressRequest) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        // Bảo mật: chỉ user chủ sở hữu mới có thể sửa
        if (!address.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized");
        }

        address.setStreetAddress(addressRequest.getStreetAddress());
        address.setWard(addressRequest.getWard());
        address.setDistrict(addressRequest.getDistrict());
        address.setCity(addressRequest.getCity());
        address.setPostalCode(addressRequest.getPostalCode());
        address.setPhone(addressRequest.getPhone());
        address.setRecipientName(addressRequest.getRecipientName());
        address.setUpdatedAt(LocalDateTime.now());

        // Nếu set làm default, unset các địa chỉ khác
        if (addressRequest.getIsDefault() != null && addressRequest.getIsDefault()) {
            addressRepository.findByUserAndIsDefaultTrue(address.getUser())
                    .ifPresent(defaultAddr -> {
                        if (!defaultAddr.getId().equals(id)) {
                            defaultAddr.setIsDefault(false);
                            addressRepository.save(defaultAddr);
                        }
                    });
            address.setIsDefault(true);
        }

        Address updated = addressRepository.save(address);
        log.info("✅ Address updated: {}", id);
        return updated;
    }

    /* ===== DELETE ===== */
    @Transactional
    public void deleteAddress(Long id, String username) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        // Bảo mật
        if (!address.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized");
        }

        // Không xóa nếu là default duy nhất
        List<Address> userAddresses = addressRepository.findByUser(address.getUser());
        if (address.getIsDefault() && userAddresses.size() == 1) {
            throw new RuntimeException("Cannot delete the only default address");
        }

        addressRepository.deleteById(id);
        log.info("✅ Address deleted: {}", id);
    }

    /* ===== SET DEFAULT ===== */
    @Transactional
    public Address setDefaultAddress(Long id, String username) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        // Bảo mật
        if (!address.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized");
        }

        // Unset default của các địa chỉ khác
        addressRepository.findByUserAndIsDefaultTrue(address.getUser())
                .ifPresent(defaultAddr -> {
                    if (!defaultAddr.getId().equals(id)) {
                        defaultAddr.setIsDefault(false);
                        addressRepository.save(defaultAddr);
                    }
                });

        address.setIsDefault(true);
        Address updated = addressRepository.save(address);
        log.info("✅ Default address set: {}", id);
        return updated;
    }
}
