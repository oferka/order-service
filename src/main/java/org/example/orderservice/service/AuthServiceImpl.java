package org.example.orderservice.service;

import org.example.orderservice.audit.AuditLogger;
import org.example.orderservice.dto.CustomerResponse;
import org.example.orderservice.dto.TokenRequest;
import org.example.orderservice.dto.TokenResponse;
import org.example.orderservice.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {

    private final CustomerService customerService;
    private final JwtService jwtService;
    private final AuditLogger auditLogger;

    public AuthServiceImpl(CustomerService customerService, JwtService jwtService, AuditLogger auditLogger) {
        this.customerService = customerService;
        this.jwtService = jwtService;
        this.auditLogger = auditLogger;
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse authenticate(TokenRequest request) {
        try {
            CustomerResponse customer = customerService.verifyCredentials(request.email(), request.password());
            auditLogger.logAuthSuccess(request.email());
            String token = jwtService.generateToken(
                    customer.id().toString(),
                    customer.email(),
                    List.of(customer.role().name())
            );
            return new TokenResponse(token);
        } catch (BadCredentialsException e) {
            auditLogger.logAuthFailure(request.email());
            throw e;
        }
    }
}
