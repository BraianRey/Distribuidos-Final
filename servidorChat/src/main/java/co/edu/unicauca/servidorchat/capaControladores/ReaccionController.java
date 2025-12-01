package co.edu.unicauca.servidorchat.capaControladores;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import co.edu.unicauca.servidorchat.capaFachadaServices.ReaccionServiceInt;
import co.edu.unicauca.servidorchat.capaFachadaServices.DTO.MensajePrivadoDTO;

@RestController
@RequestMapping("/api/reacciones")
public class ReaccionController {

    @Autowired
    private ReaccionServiceInt reaccionService;

    @PostMapping("/procesar")
    public ResponseEntity<String> procesarReaccion(@RequestBody MensajePrivadoDTO mensaje) {
        try {
            System.out.println("Reacción recibida: " + mensaje);
            reaccionService.procesarReaccion(mensaje);
            return ResponseEntity.ok("Reacción procesada exitosamente");
        } catch (Exception e) {
            System.err.println("Error procesando reacción: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + e.getMessage());
        }
    }
}
