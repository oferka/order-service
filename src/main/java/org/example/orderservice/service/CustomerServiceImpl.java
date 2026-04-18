package org.example.orderservice.service;

import org.example.orderservice.dto.CreateCustomerRequest;
import org.example.orderservice.dto.CustomerResponse;
import org.example.orderservice.exception.EntityNotFoundException;
import org.example.orderservice.mapper.CustomerMapper;
import org.example.orderservice.model.Customer;
import org.example.orderservice.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerServiceImpl.class);

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final PasswordEncoder passwordEncoder;

    public CustomerServiceImpl(CustomerRepository customerRepository,
                               CustomerMapper customerMapper,
                               PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        if (customerRepository.existsByEmail(request.email())) {
            throw new IllegalStateException("A customer with email already exists: " + request.email());
        }
        Customer customer = customerMapper.toEntity(request);
        customer.setPasswordHash(passwordEncoder.encode(request.password()));
        Customer saved = customerRepository.save(customer);
        log.info("Customer created: id={}", saved.getId());
        return customerMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse verifyCredentials(String email, String rawPassword) {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(rawPassword, customer.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        return customerMapper.toResponse(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(UUID id) {
        return customerRepository.findById(id)
                .map(customerMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Customer", id));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByEmail(String email) {
        return customerRepository.findByEmail(email)
                .map(customerMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Customer with email: " + email));
    }
}
