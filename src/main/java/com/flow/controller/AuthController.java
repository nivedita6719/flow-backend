
package com.flow.controller;

import com.flow.config.SecurityUtils;
import com.flow.dto.ApiResponse;
import com.flow.dto.AuthResponse;
import com.flow.dto.LoginRequest;
import com.flow.dto.RegisterRequest;
import com.flow.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(
        name = "Authentication",
        description = "Register and login endpoints. " +
                "Login returns JWT token for all protected APIs."
)
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(
            summary = "Register new user",
            description = "Creates a new user account " +
                    "and returns JWT token"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "User registered successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Email already registered"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation error"
            )
    })
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {

        AuthResponse response =
                authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "User registered successfully",
                                response
                        )
                );
    }

    @PostMapping("/login")
    @Operation(
            summary = "Login user",
            description = "Authenticates user credentials " +
                    "and returns JWT token. " +
                    "Use token in Authorization header: " +
                    "Bearer {token}"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Login successful"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Invalid email or password"
            )
    })
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {

        AuthResponse response =
                authService.login(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Login successful",
                        response
                )
        );
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get current user",
            description = "Returns currently authenticated " +
                    "user ID using JWT token"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Authenticated user returned"
    )
    public ResponseEntity<ApiResponse<String>> getCurrentUser() {

        String userId =
                SecurityUtils.getCurrentUserId();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Authenticated user",
                        userId
                )
        );
    }
}