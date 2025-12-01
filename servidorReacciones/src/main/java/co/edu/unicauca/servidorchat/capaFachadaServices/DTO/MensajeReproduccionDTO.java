package co.edu.unicauca.servidorchat.capaFachadaServices.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MensajeReproduccionDTO {
    private String nickname;
    private Integer idCancion;
    private Boolean reproduciendo;
}
