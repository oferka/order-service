package org.example.orderservice.config;

import jakarta.servlet.http.HttpServletResponse;
import org.example.orderservice.filter.JwtAuthenticationFilter;
import org.example.orderservice.filter.RateLimitingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
        "/api/v1/auth/**",
        "/api/v1/customers",
        "/actuator/health",
        "/actuator/info",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitingFilter rateLimitingFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RateLimitingFilter rateLimitingFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitingFilter = rateLimitingFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_PATHS).permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
            )
            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
