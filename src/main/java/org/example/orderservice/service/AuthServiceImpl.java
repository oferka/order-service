package org.example.orderservice.service;

import org.example.orderservice.dto.CustomerResponse;
import org.example.orderservice.dto.TokenRequest;
import org.example.orderservice.dto.TokenResponse;
import org.example.orderservice.security.JwtService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String ROLE_USER = "ROLE_USER";

    private final CustomerService customerService;
    private final JwtService jwtService;

    public AuthServiceImpl(CustomerService customerService, JwtService jwtService) {
        this.customerService = customerService;
        this.jwtService = jwtService;
    }

    @Override
    public TokenResponse authenticate(TokenRequest request) {
        CustomerResponse customer = customerService.verifyCredentials(request.email(), request.password());
        String token = jwtService.generateToken(
                customer.id().toString(),
                customer.email(),
                List.of(ROLE_USER)
        );
        return new TokenResponse(token);
    }
}
