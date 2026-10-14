package com.satelite.alertas.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "alertas")
@Getter
@Setter
@NoArgsConstructor
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoAlerta tipo;

    @NotBlank
    @Column(nullable = false, length = 500)
    private String mensaje;

    @Column(length = 100)
    private String fuente;

    @Column(name = "referencia_id")
    private Long referenciaId;

    @Column(name = "enviado_at")
    private LocalDateTime enviadoAt;

    @PrePersist
    protected void onCreate() {
        enviadoAt = LocalDateTime.now();
    }

    public Alerta(TipoAlerta tipo, String mensaje, String fuente, Long referenciaId) {
        this.tipo = tipo;
        this.mensaje = mensaje;
        this.fuente = fuente;
        this.referenciaId = referenciaId;
    }
}
