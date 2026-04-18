package org.example.orderservice.service;

import org.example.orderservice.dto.TokenRequest;
import org.example.orderservice.dto.TokenResponse;

public interface AuthService {

    TokenResponse authenticate(TokenRequest request);
}
