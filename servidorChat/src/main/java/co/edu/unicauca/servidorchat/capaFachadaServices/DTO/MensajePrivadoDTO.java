package co.edu.unicauca.servidorchat.capaFachadaServices.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MensajePrivadoDTO {
    private String nicknameOrigen;
    private int idCancion;
    private String contenido;
    private String reaction; // Tipo de reacción: like, love, angry, fun
}