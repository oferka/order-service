package org.example.orderservice.service;

import org.example.orderservice.dto.CreateCustomerRequest;
import org.example.orderservice.dto.CustomerResponse;

import java.util.UUID;

public interface CustomerService {

    CustomerResponse createCustomer(CreateCustomerRequest request);

    CustomerResponse getCustomerById(UUID id);

    CustomerResponse getCustomerByEmail(String email);

    CustomerResponse verifyCredentials(String email, String rawPassword);
}
