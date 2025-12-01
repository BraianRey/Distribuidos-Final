package co.edu.unicauca.banco.accesoADatos.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OperacionToken {
    private String token;
    private boolean usado;
    private LocalDateTime fechaCreacion;
    private double montoRetirado;
    private double costoGeneracion;
    private String nombreCliente;
}
