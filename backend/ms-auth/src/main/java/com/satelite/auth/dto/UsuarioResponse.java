package com.satelite.auth.dto;

import com.satelite.auth.entity.Usuario;

import java.time.LocalDateTime;

public record UsuarioResponse(Long id, String nombre, String username, String rol,
                              boolean enabled, LocalDateTime ultimoAcceso) {

    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNombre(), u.getUsername(), u.getRol().name(),
                u.isEnabled(), u.getUltimoAcceso());
    }
}
