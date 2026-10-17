package com.satelite.auth.controller;

import com.satelite.auth.dto.LoginRequest;
import com.satelite.auth.dto.LoginResponse;
import com.satelite.auth.dto.RegisterRequest;
import com.satelite.auth.entity.Usuario;
import com.satelite.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @RequestHeader(value = "X-Auth-Role", required = false) String rolSolicitante,
            @Valid @RequestBody RegisterRequest request) {
        if (!"ADMIN".equals(rolSolicitante)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Solo un ADMIN puede registrar usuarios"));
        }
        Usuario usuario = authService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "mensaje", "Usuario creado exitosamente",
                "id", usuario.getId(),
                "username", usuario.getUsername(),
                "rol", usuario.getRol().name()
        ));
    }
}
