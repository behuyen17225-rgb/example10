package com.nguyenthithuhuyen.example10.repository;

import com.nguyenthithuhuyen.example10.entity.Address;
import com.nguyenthithuhuyen.example10.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    // Lấy tất cả địa chỉ của user
    List<Address> findByUser(User user);
    
    // Lấy tất cả địa chỉ của user theo username
    List<Address> findByUser_Username(String username);
    
    // Lấy địa chỉ mặc định của user
    Optional<Address> findByUserAndIsDefaultTrue(User user);
    
    // Xóa tất cả địa chỉ của user
    void deleteByUser(User user);
}
