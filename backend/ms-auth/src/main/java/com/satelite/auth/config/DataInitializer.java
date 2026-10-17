package com.satelite.auth.config;

import com.satelite.auth.entity.RolUsuario;
import com.satelite.auth.entity.Usuario;
import com.satelite.auth.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!usuarioRepository.existsByUsername("admin")) {
            Usuario admin = new Usuario(
                    "admin",
                    passwordEncoder.encode("admin123"),
                    RolUsuario.ADMIN,
                    "Administrador"
            );
            usuarioRepository.save(admin);
            log.info("Usuario admin creado: admin / admin123");
        }

        if (!usuarioRepository.existsByUsername("operador")) {
            Usuario operador = new Usuario(
                    "operador",
                    passwordEncoder.encode("op123"),
                    RolUsuario.OPERADOR,
                    "Operador de Almacén"
            );
            usuarioRepository.save(operador);
            log.info("Usuario operador creado: operador / op123");
        }

        if (!usuarioRepository.existsByUsername("analista")) {
            Usuario analista = new Usuario(
                    "analista",
                    passwordEncoder.encode("an123"),
                    RolUsuario.ANALISTA,
                    "Analista de Compras"
            );
            usuarioRepository.save(analista);
            log.info("Usuario analista creado: analista / an123");
        }
    }
}
