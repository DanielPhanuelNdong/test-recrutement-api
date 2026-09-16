package com.test.recutement_test.auth;

import com.test.recutement_test.auth.dto.AuthResponse;
import com.test.recutement_test.auth.dto.RegisterRequest;
import com.test.recutement_test.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapping entité/DTO pour {@link User}. Le hachage du mot de passe reste porté par
 * {@link AuthService} (règle de sécurité, pas une correspondance de champs) : le mapper
 * ignore volontairement {@code password}.
 */
@Mapper(componentModel = "spring")
public interface AuthMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    User toEntity(RegisterRequest request);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "fullName", source = "user.fullName")
    @Mapping(target = "token", source = "token")
    @Mapping(target = "expiresIn", source = "expiresInSeconds")
    @Mapping(target = "tokenType", constant = "Bearer")
    AuthResponse toAuthResponse(User user, String token, long expiresInSeconds);
}
