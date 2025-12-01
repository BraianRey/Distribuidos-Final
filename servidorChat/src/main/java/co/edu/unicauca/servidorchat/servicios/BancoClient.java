package co.edu.unicauca.servidorchat.servicios;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import feign.FeignException;

import java.util.Map;

/**
 * Cliente Feign para comunicación con servidorBanco.
 * Definición de endpoints sin reintentos (el reintento se maneja en la capa service).
 * Usa la configuración personalizada de FeignConfig.
 */
@FeignClient(name = "bancoClient", url = "http://localhost:8082/api/operaciones", configuration = FeignConfig.class)
public interface BancoClient {

    /**
     * Genera un token en el banco
     */
    @PostMapping("/generarToken")
    String generarToken(@RequestHeader("Nickname") String nombreCliente) throws FeignException;

    /**
     * Procesa un pago en el banco
     */
    @PostMapping(value = "/pago", consumes = MediaType.APPLICATION_JSON_VALUE)
    String procesarPago(@RequestHeader("Operacion-Token") String token, @RequestBody Map<String, Object> body) throws FeignException;
}
