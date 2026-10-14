package com.satelite.auth.controller;

import com.satelite.auth.dto.UsuarioRequest;
import com.satelite.auth.dto.UsuarioResponse;
import com.satelite.auth.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;

import java.util.List;

// X-Auth-User / X-Auth-Role los inyecta ms-gateway a partir del JWT
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    public List<UsuarioResponse> findAll(@RequestHeader(value = "X-Auth-Role", required = false) String rol) {
        soloAdmin(rol);
        return usuarioService.findAll();
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> create(@RequestHeader(value = "X-Auth-Role", required = false) String rol,
                                                  @Valid @RequestBody UsuarioRequest request) {
        soloAdmin(rol);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.create(request));
    }

    @PutMapping("/{id}")
    public UsuarioResponse update(@RequestHeader(value = "X-Auth-Role", required = false) String rol,
                                  @PathVariable Long id, @Valid @RequestBody UsuarioRequest request) {
        soloAdmin(rol);
        return usuarioService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestHeader(value = "X-Auth-Role", required = false) String rol,
                                       @RequestHeader(value = "X-Auth-User", required = false) String username,
                                       @PathVariable Long id) {
        soloAdmin(rol);
        usuarioService.delete(id, username);
        return ResponseEntity.noContent().build();
    }

    private void soloAdmin(String rol) {
        if (!"ADMIN".equals(rol)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo un ADMIN puede gestionar usuarios");
        }
    }
}
