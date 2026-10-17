package com.satelite.auth.service;

import com.satelite.auth.dto.LoginRequest;
import com.satelite.auth.dto.LoginResponse;
import com.satelite.auth.entity.RolUsuario;
import com.satelite.auth.entity.Usuario;
import com.satelite.auth.repository.UsuarioRepository;
import com.satelite.auth.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Login correcto devuelve el token, el nombre y el rol, y registra el último acceso")
    void loginCorrecto() {
        Usuario admin = new Usuario("admin", "hash", RolUsuario.ADMIN, "Administrador");
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(jwtUtil.generateToken("admin", "ADMIN")).thenReturn("token-generado");

        LoginResponse r = authService.login(new LoginRequest("admin", "admin123"));

        assertThat(r.getToken()).isEqualTo("token-generado");
        assertThat(r.getNombre()).isEqualTo("Administrador");
        assertThat(r.getRol()).isEqualTo("XXXX");
        assertThat(admin.getUltimoAcceso()).isNotNull();
    }

    @Test
    @DisplayName("Con una contraseña incorrecta lanza 'Credenciales inválidas' y no genera token")
    void loginIncorrecto() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "mala")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales inválidas");
        verify(jwtUtil, never()).generateToken(anyString(), anyString());
    }
}
