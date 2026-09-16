package com.test.recutement_test.auth;

import com.test.recutement_test.auth.dto.AuthResponse;
import com.test.recutement_test.auth.dto.LoginRequest;
import com.test.recutement_test.auth.dto.RegisterRequest;
import com.test.recutement_test.exception.ApiException;
import com.test.recutement_test.security.JwtService;
import com.test.recutement_test.user.User;
import com.test.recutement_test.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AuthMapper authMapper;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Un compte existe déjà avec cet email");
        }

        User user = authMapper.toEntity(request);
        user.setEmail(request.email().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.password()));

        User saved = userRepository.save(user);
        String token = jwtService.generateToken(saved);
        return authMapper.toAuthResponse(saved, token, jwtService.getExpirationMs() / 1000);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password()));

        User user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> ApiException.unauthorized("Email ou mot de passe invalide"));

        String token = jwtService.generateToken(user);
        return authMapper.toAuthResponse(user, token, jwtService.getExpirationMs() / 1000);
    }
}
