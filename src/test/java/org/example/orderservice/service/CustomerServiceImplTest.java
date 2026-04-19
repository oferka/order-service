package org.example.orderservice.service;

import org.example.orderservice.dto.CreateCustomerRequest;
import org.example.orderservice.dto.CustomerResponse;
import org.example.orderservice.exception.EntityNotFoundException;
import org.example.orderservice.mapper.CustomerMapper;
import org.example.orderservice.model.Customer;
import org.example.orderservice.model.CustomerRole;
import org.example.orderservice.repository.CustomerRepository;
import org.example.orderservice.security.SecurityUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private CustomerMapper customerMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private SecurityUtils securityUtils;

    @InjectMocks
    private CustomerServiceImpl customerService;

    @Nested
    class CreateCustomer {

        @Test
        void should_createCustomer_when_emailIsUnique() {
            CreateCustomerRequest request = new CreateCustomerRequest(
                    "jane@example.com", "Password1!", "Jane Doe", null);
            Customer mapped = Customer.builder().email("jane@example.com").fullName("Jane Doe").build();
            Customer saved = Customer.builder().id(UUID.randomUUID()).email("jane@example.com")
                    .fullName("Jane Doe").role(CustomerRole.ROLE_USER).build();
            CustomerResponse expected = new CustomerResponse(
                    saved.getId(), "jane@example.com", "Jane Doe", null, CustomerRole.ROLE_USER, null, null);

            when(customerRepository.existsByEmail("jane@example.com")).thenReturn(false);
            when(customerMapper.toEntity(request)).thenReturn(mapped);
            when(passwordEncoder.encode("Password1!")).thenReturn("$2a$10$hashed");
            when(customerRepository.save(mapped)).thenReturn(saved);
            when(customerMapper.toResponse(saved)).thenReturn(expected);

            CustomerResponse result = customerService.createCustomer(request);

            assertThat(result).isEqualTo(expected);
            assertThat(mapped.getPasswordHash()).isEqualTo("$2a$10$hashed");
            verify(customerRepository).save(mapped);
        }

        @Test
        void should_throwIllegalStateException_when_emailAlreadyExists() {
            CreateCustomerRequest request = new CreateCustomerRequest(
                    "existing@example.com", "Password1!", "Jane Doe", null);

            when(customerRepository.existsByEmail("existing@example.com")).thenReturn(true);

            assertThatThrownBy(() -> customerService.createCustomer(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Registration failed");

            verify(customerRepository, never()).save(any());
        }

        @Test
        void should_useGenericMessage_when_emailAlreadyExists() {
            // Verify the message does NOT expose the email (anti-enumeration)
            CreateCustomerRequest request = new CreateCustomerRequest(
                    "secret@example.com", "Password1!", "Jane Doe", null);

            when(customerRepository.existsByEmail("secret@example.com")).thenReturn(true);

            assertThatThrownBy(() -> customerService.createCustomer(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageNotContaining("secret@example.com");
        }
    }

    @Nested
    class VerifyCredentials {

        @Test
        void should_returnCustomerResponse_when_credentialsAreValid() {
            String email = "jane@example.com";
            String rawPassword = "Password1!";
            Customer customer = Customer.builder().id(UUID.randomUUID()).email(email)
                    .passwordHash("$2a$10$hashed").role(CustomerRole.ROLE_USER).build();
            CustomerResponse expected = new CustomerResponse(
                    customer.getId(), email, "Jane Doe", null, CustomerRole.ROLE_USER, null, null);

            when(customerRepository.findByEmail(email)).thenReturn(Optional.of(customer));
            when(passwordEncoder.matches(rawPassword, "$2a$10$hashed")).thenReturn(true);
            when(customerMapper.toResponse(customer)).thenReturn(expected);

            CustomerResponse result = customerService.verifyCredentials(email, rawPassword);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        void should_throwBadCredentialsException_when_emailNotFound() {
            when(customerRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.verifyCredentials("unknown@example.com", "any"))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Invalid credentials");
        }

        @Test
        void should_throwBadCredentialsException_when_passwordIsWrong() {
            Customer customer = Customer.builder().id(UUID.randomUUID())
                    .email("jane@example.com").passwordHash("$2a$10$hashed").build();

            when(customerRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(customer));
            when(passwordEncoder.matches("wrong", "$2a$10$hashed")).thenReturn(false);

            assertThatThrownBy(() -> customerService.verifyCredentials("jane@example.com", "wrong"))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Invalid credentials");
        }

        @Test
        void should_useSameErrorMessage_for_unknownEmailAndWrongPassword() {
            // Both failure cases must return identical messages to prevent email enumeration
            Customer customer = Customer.builder().id(UUID.randomUUID())
                    .email("jane@example.com").passwordHash("$2a$10$hashed").build();

            when(customerRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());
            when(customerRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(customer));
            when(passwordEncoder.matches("wrong", "$2a$10$hashed")).thenReturn(false);

            String messageForUnknownEmail = null;
            String messageForWrongPassword = null;

            try {
                customerService.verifyCredentials("unknown@example.com", "any");
            } catch (BadCredentialsException e) {
                messageForUnknownEmail = e.getMessage();
            }

            try {
                customerService.verifyCredentials("jane@example.com", "wrong");
            } catch (BadCredentialsException e) {
                messageForWrongPassword = e.getMessage();
            }

            assertThat(messageForUnknownEmail).isEqualTo(messageForWrongPassword);
        }
    }

    @Nested
    class GetCustomerById {

        @Test
        void should_returnCustomer_when_callerIsOwner() {
            UUID id = UUID.randomUUID();
            Customer customer = Customer.builder().id(id).email("jane@example.com").build();
            CustomerResponse expected = new CustomerResponse(
                    id, "jane@example.com", "Jane Doe", null, CustomerRole.ROLE_USER, null, null);

            when(securityUtils.isAdmin()).thenReturn(false);
            when(securityUtils.getCurrentUserId()).thenReturn(id);
            when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
            when(customerMapper.toResponse(customer)).thenReturn(expected);

            CustomerResponse result = customerService.getCustomerById(id);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        void should_returnCustomer_when_callerIsAdmin() {
            UUID id = UUID.randomUUID();
            Customer customer = Customer.builder().id(id).email("jane@example.com").build();
            CustomerResponse expected = new CustomerResponse(
                    id, "jane@example.com", "Jane Doe", null, CustomerRole.ROLE_USER, null, null);

            when(securityUtils.isAdmin()).thenReturn(true);
            when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
            when(customerMapper.toResponse(customer)).thenReturn(expected);

            CustomerResponse result = customerService.getCustomerById(id);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        void should_throwAccessDeniedException_when_callerIsNonOwner() {
            UUID id = UUID.randomUUID();

            when(securityUtils.isAdmin()).thenReturn(false);
            when(securityUtils.getCurrentUserId()).thenReturn(UUID.randomUUID());

            assertThatThrownBy(() -> customerService.getCustomerById(id))
                    .isInstanceOf(AccessDeniedException.class);

            verify(customerRepository, never()).findById(any());
        }

        @Test
        void should_throwEntityNotFoundException_when_customerNotFound() {
            UUID id = UUID.randomUUID();

            when(securityUtils.isAdmin()).thenReturn(true);
            when(customerRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.getCustomerById(id))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    class GetCustomerByEmail {

        @Test
        void should_returnCustomer_when_emailExists() {
            String email = "jane@example.com";
            Customer customer = Customer.builder().id(UUID.randomUUID()).email(email).build();
            CustomerResponse expected = new CustomerResponse(
                    customer.getId(), email, "Jane Doe", null, CustomerRole.ROLE_USER, null, null);

            when(customerRepository.findByEmail(email)).thenReturn(Optional.of(customer));
            when(customerMapper.toResponse(customer)).thenReturn(expected);

            CustomerResponse result = customerService.getCustomerByEmail(email);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        void should_throwEntityNotFoundException_when_emailNotFound() {
            when(customerRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.getCustomerByEmail("nobody@example.com"))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }
}
