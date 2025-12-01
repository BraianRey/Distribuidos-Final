package co.edu.unicauca.banco.capaControladores;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unicauca.banco.capaFachadaServices.OperacionServiceInt;
import co.edu.unicauca.banco.capaFachadaServices.DTOS.OperacionRetiroDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/operaciones")
public class OperacionController {

   
   @Autowired
    private ContadorFallos contadorFallos;

    @Autowired
    private OperacionServiceInt service;
   
    @PostMapping("/generarToken")
    public ResponseEntity<String> generarToken(@RequestHeader("Nickname") String nombreCliente) {
       // Simula un posible fallo en la generación del token
        int intento = contadorFallos.siguienteIntento();
        System.out.println("Intento de generar token #" + intento);

        // Si aún estamos en la secuencia inicial de fallos, simularlos
        if (contadorFallos.shouldSimulateInitialFailure(intento)) {
            System.out.println("Simulando fallo inicial en generarToken (intento " + intento + ")...");
            throw new RuntimeException("Fallo simulado en intento " + intento);
        }

        String token = service.generarToken(nombreCliente);
        ResponseEntity<String> response = ResponseEntity.ok(token);
        // Si llegamos aquí es porque la operación tuvo éxito; si estabamos en la secuencia inicial,
        // marcarla como completada para que futuras operaciones no simulen fallos.
        contadorFallos.markInitialSequenceDone();
        return response;
    }

    @PostMapping("/pago")
    public ResponseEntity<String> pago(@RequestHeader("Operacion-Token") String token,
                                          @RequestBody OperacionRetiroDTO operacion) {
        int intento = contadorFallos.siguienteIntento();
        System.out.println("Intento de pago #" + intento);

        if (contadorFallos.shouldSimulateInitialFailure(intento)) {
            System.out.println("Simulando fallo inicial en pago (intento " + intento + ")...");
            throw new RuntimeException("Fallo simulado en intento " + intento);
        }

        String respuesta=this.service.procesarPago(token, operacion.getMonto(), operacion.getNombreCliente());
        ResponseEntity<String> response = ResponseEntity.ok(respuesta +" "+ intento);
        // Si la operación fue exitosa y estábamos en la secuencia inicial, marcarla como completada
        contadorFallos.markInitialSequenceDone();
        return response;
    }

    @GetMapping("/MontosPagados")
    public List<Double> getMontosPagados(@RequestParam String nombreCliente)
    {
        return service.getMontosPagados(nombreCliente);
    }

}

