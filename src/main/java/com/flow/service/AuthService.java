package com.flow.service;

import com.flow.dto.AuthResponse;
import com.flow.dto.LoginRequest;
import com.flow.dto.RegisterRequest;
import com.flow.exception.AppException;
import com.flow.model.User;
import com.flow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(
                    "Email already registered",
                    HttpStatus.CONFLICT
            );
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(hashedPassword)
                .build();

        User savedUser = userRepository.save(user);

        // Register pe bhi token generate karo
        String token = jwtService.generateToken(
                savedUser.getId(),
                savedUser.getEmail()
        );

        return new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getName()
        );
    }

    public AuthResponse login(LoginRequest request) {

        // Step 1: Email se user dhundo
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(
                        "Invalid email or password",
                        HttpStatus.UNAUTHORIZED
                ));

        // Step 2: Password verify karo
        // BCrypt entered password ko hash karke compare karta hai
        boolean passwordMatches = passwordEncoder.matches(
                request.getPassword(),
                user.getPasswordHash()
        );

        if (!passwordMatches) {
            throw new AppException(
                    "Invalid email or password",
                    HttpStatus.UNAUTHORIZED
            );
        }

        // Step 3: JWT token generate karo
        String token = jwtService.generateToken(
                user.getId(),
                user.getEmail()
        );

        return new AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getName()
        );
    }
}
