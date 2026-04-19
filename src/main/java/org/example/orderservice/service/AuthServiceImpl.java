package org.example.orderservice.service;

import org.example.orderservice.dto.CustomerResponse;
import org.example.orderservice.dto.TokenRequest;
import org.example.orderservice.dto.TokenResponse;
import org.example.orderservice.security.JwtService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {

    private final CustomerService customerService;
    private final JwtService jwtService;

    public AuthServiceImpl(CustomerService customerService, JwtService jwtService) {
        this.customerService = customerService;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse authenticate(TokenRequest request) {
        CustomerResponse customer = customerService.verifyCredentials(request.email(), request.password());
        String token = jwtService.generateToken(
                customer.id().toString(),
                customer.email(),
                List.of(customer.role().name())
        );
        return new TokenResponse(token);
    }
}
