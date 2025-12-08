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
        System.out.println("Intento de generar token #" + intento + " (nickname=" + nombreCliente + ")");

        // Si aún estamos en la secuencia inicial de fallos, simularlos
        if (contadorFallos.shouldSimulateInitialFailure(intento)) {
            System.out.println("Simulando fallo inicial en generarToken (intento " + intento + ")...");
            // Simular retardo mayor al readTimeout del cliente para provocar timeout
            try {
                Thread.sleep(4000); // > cliente readTimeout (3s)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Sleep interrumpido en generarToken: " + e.getMessage());
            }
            // El sleep (> readTimeout del cliente) hace que el cliente experimente timeout.
            System.out.println("[OperacionController] Simulación de no-respuesta completada para intento " + intento);
            return ResponseEntity.status(503).body("Servicio temporalmente no disponible");
        }

        String token = service.generarToken(nombreCliente);
        System.out.println("Token generado: " + token + " para cliente=" + nombreCliente);
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
        System.out.println("Intento de pago #" + intento + " (token=" + token + ")");

        if (contadorFallos.shouldSimulateInitialFailure(intento)) {
            System.out.println("Simulando fallo inicial en pago (intento " + intento + ")...");
            // Simular retardo mayor al readTimeout del cliente para provocar timeout
            try {
                Thread.sleep(4000); // > cliente readTimeout (3s)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Sleep interrumpido en pago: " + e.getMessage());
            }
            System.out.println("[OperacionController] Simulación de no-respuesta completada en pago para intento " + intento);
            return ResponseEntity.status(503).body("Servicio temporalmente no disponible (simulado)");
        }

        System.out.println("Procesando pago con monto=" + operacion.getMonto() + " nombreCliente=" + operacion.getNombreCliente());
        String respuesta=this.service.procesarPago(token, operacion.getMonto(), operacion.getNombreCliente());
        ResponseEntity<String> response = ResponseEntity.ok(respuesta +" "+ intento);
        // Si la operación fue exitosa y estábamos en la secuencia inicial, marcarla como completada
        contadorFallos.markInitialSequenceDone();
        System.out.println("Pago procesado correctamente: " + respuesta + " (intento=" + intento + ")");
        return response;
    }

    @GetMapping("/MontosPagados")
    public List<Double> getMontosPagados(@RequestParam String nombreCliente)
    {
        return service.getMontosPagados(nombreCliente);
    }

}

