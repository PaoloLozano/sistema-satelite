package com.satelite.lotes.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "lotes")
@Getter
@Setter
@NoArgsConstructor
public class Lote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String ean;

    @NotBlank
    @Column(name = "producto_nombre", nullable = false, length = 200)
    private String productoNombre;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "costo_unitario", precision = 10, scale = 2)
    private BigDecimal costoUnitario;

    @NotNull
    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(length = 150)
    private String proveedor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoLote estado;

    @Column(name = "creado_en")
    private LocalDateTime creadoEn;

    @PrePersist
    @PreUpdate
    protected void calcularEstado() {
        creadoEn = (creadoEn != null) ? creadoEn : LocalDateTime.now();
        actualizarEstado();
    }

    public void actualizarEstado() {
        if (fechaVencimiento != null) {
            long dias = ChronoUnit.DAYS.between(LocalDate.now(), fechaVencimiento);
            if (dias <= 7) {
                this.estado = EstadoLote.ROJO;
            } else if (dias <= 30) {
                this.estado = EstadoLote.AMARILLO;
            } else {
                this.estado = EstadoLote.VERDE;
            }
        }
    }

    public long getDiasRestantes() {
        if (fechaVencimiento == null) return Long.MAX_VALUE;
        return ChronoUnit.DAYS.between(LocalDate.now(), fechaVencimiento);
    }
}
