package com.satelite.auth.service;

import com.satelite.auth.dto.UsuarioRequest;
import com.satelite.auth.dto.UsuarioResponse;
import com.satelite.auth.entity.Usuario;
import com.satelite.auth.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UsuarioResponse> findAll() {
        return usuarioRepository.findAll().stream().map(UsuarioResponse::from).toList();
    }

    @Transactional
    public UsuarioResponse create(UsuarioRequest req) {
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }
        if (usuarioRepository.existsByUsername(req.getUsername())) {
            throw new IllegalArgumentException("El usuario '" + req.getUsername() + "' ya existe");
        }
        Usuario u = new Usuario(req.getUsername(), passwordEncoder.encode(req.getPassword()), req.getRol(), req.getNombre());
        u.setEnabled(req.isEnabled());
        return UsuarioResponse.from(usuarioRepository.save(u));
    }

    @Transactional
    public UsuarioResponse update(Long id, UsuarioRequest req) {
        Usuario u = findEntity(id);
        if (!u.getUsername().equals(req.getUsername()) && usuarioRepository.existsByUsername(req.getUsername())) {
            throw new IllegalArgumentException("El usuario '" + req.getUsername() + "' ya existe");
        }
        u.setNombre(req.getNombre());
        u.setUsername(req.getUsername());
        u.setRol(req.getRol());
        u.setEnabled(req.isEnabled());
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            u.setPassword(passwordEncoder.encode(req.getPassword()));
        }
        return UsuarioResponse.from(u);
    }

    @Transactional
    public void delete(Long id, String usernameSolicitante) {
        Usuario u = findEntity(id);
        if (u.getUsername().equals(usernameSolicitante)) {
            throw new IllegalArgumentException("No puedes eliminar tu propio usuario");
        }
        usuarioRepository.delete(u);
    }

    private Usuario findEntity(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con id: " + id));
    }
}
