package co.edu.unicauca.servidorchat.capaFachadaServices.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MensajePrivadoDTO {
    private String nicknameOrigen;
    private int idCancion;
    private String contenido;
    private String reaction; // Tipo de reacción: like, love, angry, fun
}