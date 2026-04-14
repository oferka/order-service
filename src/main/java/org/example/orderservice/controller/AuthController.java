package org.example.orderservice.controller;

import jakarta.validation.Valid;
import org.example.orderservice.dto.TokenRequest;
import org.example.orderservice.dto.TokenResponse;
import org.example.orderservice.security.JwtService;
import org.example.orderservice.service.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final CustomerService customerService;
    private final JwtService jwtService;

    public AuthController(CustomerService customerService, JwtService jwtService) {
        this.customerService = customerService;
        this.jwtService = jwtService;
    }

    @PostMapping("/token")
    public ResponseEntity<TokenResponse> generateToken(@RequestBody @Valid TokenRequest request) {
        var customer = customerService.getCustomerByEmail(request.email());
        String token = jwtService.generateToken(
                customer.id().toString(),
                customer.email(),
                List.of("ROLE_USER")
        );
        return ResponseEntity.ok(new TokenResponse(token));
    }
}
