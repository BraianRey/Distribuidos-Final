package co.edu.unicauca.servidorchat.capaFachadaServices.DTO;

import java.util.Date;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MensajePublicoDTO {
    private String contenido;
    private String nickname;
    private Date fechaGeneracion;
}

