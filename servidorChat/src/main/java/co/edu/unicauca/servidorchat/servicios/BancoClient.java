package co.edu.unicauca.servidorchat.servicios;

import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * Cliente para comunicación con servidorBanco.
 * Definición de endpoints sin reintentos (el reintento se maneja en la capa service).
 */
public interface BancoClient {

    /**
     * Genera un token en el banco
     */
    String generarToken(String nombreCliente) throws RestClientException;

    /**
     * Procesa un pago en el banco
     */
    String procesarPago(String token, Map<String, Object> body) throws RestClientException;
}
