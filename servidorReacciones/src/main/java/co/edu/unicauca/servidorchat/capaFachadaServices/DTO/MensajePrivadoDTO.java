package co.edu.unicauca.servidorchat.capaFachadaServices.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MensajePrivadoDTO {
    private String nicknameOrigen;
    private String nicknameDestino;
    private Integer idCancion;
    private String reaction;
    private String contenido;
}

