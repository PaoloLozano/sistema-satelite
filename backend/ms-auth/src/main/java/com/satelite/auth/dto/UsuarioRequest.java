package com.satelite.auth.dto;

import com.satelite.auth.entity.RolUsuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioRequest {

    @NotBlank(message = "El nombre es requerido")
    private String nombre;

    @NotBlank(message = "El username es requerido")
    private String username;

    @NotNull(message = "El rol es requerido")
    private RolUsuario rol;

    private boolean enabled = true;

    // Obligatorio al crear; opcional al editar (si viene vacío no se cambia)
    private String password;
}
