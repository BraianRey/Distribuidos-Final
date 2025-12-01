package co.edu.unicauca.servidorchat.servicios;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * Implementación del cliente Banco con reintentos automáticos.
 * Sigue el patrón de clienterest/OperacionesClienteImpl.
 */
@Service
public class BancoOperacionesImpl implements BancoClient {

    @Autowired
    private RestTemplate restTemplate;

    private static final String URL_BANCO = "http://localhost:8082/api/operaciones";
    private static final int MAX_ATTEMPTS_TOKEN = 4;
    private static final int MAX_ATTEMPTS_PAGO = 5;
    private static final long DELAY_MS = 2000;

    /**
     * Genera un token con reintentos automáticos.
     * Máximo 3 intentos con delay de 2 segundos.
     */
    @Retryable(
        retryFor = {RestClientException.class},
        maxAttempts = MAX_ATTEMPTS_TOKEN,
        backoff = @Backoff(delay = DELAY_MS)
    )
    @Override
    public String generarToken(String nombreCliente) throws RestClientException {
        System.out.println("🔄 Intentando generar token para cliente: " + nombreCliente);
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Nickname", nombreCliente);
        HttpEntity<String> request = new HttpEntity<>(null, headers);
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                URL_BANCO + "/generarToken",
                HttpMethod.POST,
                request,
                String.class
            );
            
            String token = response.getBody();
            System.out.println("✅ Token generado exitosamente: " + token);
            return token;
        } catch (RestClientException ex) {
            System.err.println("⚠️ Error al generar token (reintentando...): " + ex.getMessage());
            throw ex;
        }
    }

    /**
     * Procesa un pago con reintentos automáticos.
     * Máximo 5 intentos con delay de 2 segundos.
     */
    @Retryable(
        retryFor = {RestClientException.class},
        maxAttempts = MAX_ATTEMPTS_PAGO,
        backoff = @Backoff(delay = DELAY_MS)
    )
    @Override
    public String procesarPago(String token, Map<String, Object> body) throws RestClientException {
        System.out.println("Intentando procesar pago con token: " + token);
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Operacion-Token", token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                URL_BANCO + "/pago",
                request,
                String.class
            );
            
            String respuesta = response.getBody();
            System.out.println("Pago procesado exitosamente: " + respuesta);
            return respuesta;
        } catch (RestClientException ex) {
            System.err.println("Error al procesar pago (reintentando...): " + ex.getMessage());
            throw ex;
        }
    }

    /**
     * Método de recuperación cuando fallan todos los reintentos para generar token.
     */
    @Recover
    public String recuperarGenerarToken(RestClientException ex, String nombreCliente) {
        System.err.println("Todos los reintentos fallaron para generar token del cliente: " + nombreCliente);
        System.err.println("Causa: " + ex.getMessage());
        return "Error: No se pudo generar token tras " + MAX_ATTEMPTS_TOKEN + " intentos";
    }

    /**
     * Método de recuperación cuando fallan todos los reintentos para pago.
     */
    @Recover
    public String recuperarProcesarPago(RestClientException ex, String token, Map<String, Object> body) {
        System.err.println("Todos los reintentos fallaron para procesar pago con token: " + token);
        System.err.println("Causa: " + ex.getMessage());
        return "Error: No se pudo procesar pago tras " + MAX_ATTEMPTS_PAGO + " intentos";
    }
}
