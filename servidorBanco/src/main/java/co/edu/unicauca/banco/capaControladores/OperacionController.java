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
    public ResponseEntity<String> generarToken() {
       // Simula un posible fallo en la generación del token
        int intento = contadorFallos.siguienteIntento();
        System.out.println("Intento de generar token #" + intento);
  
        if (intento == 1)
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        String token = service.generarToken();
        return ResponseEntity.ok(token);
    }

    @PostMapping("/retirar")
    public ResponseEntity<String> retirar(@RequestHeader("Operacion-Token") String token,
                                          @RequestBody OperacionRetiroDTO operacion) {
        int intento = contadorFallos.siguienteIntento();
        System.out.println("Intento de retiro #" + intento);
  
        if (intento < 4) {
            // Simula un fallo (por ejemplo, retardo que cause timeout o una excepción)
            try { 
                System.out.println("Simulando fallo...");
                Thread.sleep(8000); // mayor al timeout del cliente (por ejemplo, 5s)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException("Fallo simulado");
        }
        String respuesta=this.service.procesarRetiro(token, operacion.getMonto(), operacion.getIdCliente());
        return ResponseEntity.ok(respuesta +" "+ intento);
    }

    @GetMapping("/MontosRetirados")
    public List<Double> getMontosRetirados(@RequestParam int idCliente)
    {
        return service.getMontosRetirados(idCliente);
    }

}

