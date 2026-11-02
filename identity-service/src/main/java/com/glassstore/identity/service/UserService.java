package com.glassstore.identity.service;

import com.glassstore.identity.dto.*;

import java.util.List;

public interface UserService {
    List<UserDTO> getAllStaff();
    List<UserDTO> getAllCustomers();
    UserDTO getUserById(Long id);
    UserDTO createStaff(CreateStaffRequest request);
    UserDTO createCustomer(CreateCustomerRequest request);
    UserDTO toggleUserStatus(Long id);
    LoginResponse login(LoginRequest request);
}
