package com.test.recutement_test.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.test.recutement_test.auth.dto.AuthResponse;
import com.test.recutement_test.auth.dto.RegisterRequest;
import com.test.recutement_test.user.User;
import org.junit.jupiter.api.Test;

class AuthMapperTest {

    private final AuthMapper mapper = new AuthMapperImpl();

    @Test
    void toEntityIgnoresPasswordIdAndCreatedAt() {
        RegisterRequest request = new RegisterRequest("Alice Doe", "alice@example.com", "password123");

        User user = mapper.toEntity(request);

        assertThat(user.getFullName()).isEqualTo("Alice Doe");
        assertThat(user.getEmail()).isEqualTo("alice@example.com");
        // Password hashing and normalization are security concerns owned by AuthService.
        assertThat(user.getPassword()).isNull();
        assertThat(user.getId()).isNull();
        assertThat(user.getCreatedAt()).isNull();
    }

    @Test
    void toAuthResponseAssemblesUserAndTokenData() {
        User user = User.builder().id(1L).email("alice@example.com").fullName("Alice Doe").build();

        AuthResponse response = mapper.toAuthResponse(user, "jwt-token", 86_400L);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(86_400L);
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("alice@example.com");
        assertThat(response.fullName()).isEqualTo("Alice Doe");
    }
}
