package org.example.orderservice.controller;

import jakarta.validation.Valid;
import org.example.orderservice.dto.TokenRequest;
import org.example.orderservice.dto.TokenResponse;
import org.example.orderservice.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/token")
    public ResponseEntity<TokenResponse> generateToken(@RequestBody @Valid TokenRequest request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }
}
