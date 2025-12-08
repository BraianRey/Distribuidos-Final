package co.edu.unicauca.servidorchat.servicios;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import feign.FeignException;
import feign.RetryableException;

import java.util.Map;

/**
 * Implementación del cliente Banco con reintentos automáticos.
 * Usa Spring Cloud Feign como cliente HTTP.
 */
@Service
public class BancoOperacionesImpl {

    @Autowired
    private BancoClient bancoClient;

    private static final int MAX_ATTEMPTS_TOKEN = 5;
    private static final int MAX_ATTEMPTS_PAGO = 5;
    private static final long DELAY_MS = 2000;

    /**
     * Genera un token con reintentos automáticos.
     * Máximo 5 intentos con delay de 2 segundos.
     */
    @Retryable(
        retryFor = {FeignException.class, RetryableException.class},
        maxAttempts = MAX_ATTEMPTS_TOKEN,
        backoff = @Backoff(delay = DELAY_MS)
    )
    public String generarToken(String nombreCliente) throws FeignException {
        System.out.println("Intentando generar token para cliente: " + nombreCliente);
        String token = bancoClient.generarToken(nombreCliente);
        System.out.println("Token generado exitosamente: " + token);
        return token;
    }

    /**
     * Procesa un pago con reintentos automáticos.
     * Máximo 5 intentos con delay de 2 segundos.
     */
    @Retryable(
        retryFor = {FeignException.class, RetryableException.class},
        maxAttempts = MAX_ATTEMPTS_PAGO,
        backoff = @Backoff(delay = DELAY_MS)
    )
    public String procesarPago(String token, Map<String, Object> body) throws FeignException {
        System.out.println("Intentando procesar pago con token: " + token);
        String respuesta = bancoClient.procesarPago(token, body);
        System.out.println("Pago procesado exitosamente: " + respuesta);
        return respuesta;
    }

    /**
     * Método de recuperación cuando fallan todos los reintentos para generar token.
     */
    @Recover
    public String recuperarGenerarToken(FeignException ex, String nombreCliente) {
        System.err.println("Todos los reintentos fallaron para generar token del cliente: " + nombreCliente);
        System.err.println("Causa: " + ex.getMessage());
        return "Error: No se pudo generar token tras " + MAX_ATTEMPTS_TOKEN + " intentos";
    }

    @Recover
    public String recuperarGenerarToken(RetryableException ex, String nombreCliente) {
        System.err.println("Todos los reintentos fallaron (timeout) para generar token del cliente: " + nombreCliente);
        System.err.println("Causa: " + ex.getMessage());
        return "Error: No se pudo generar token tras " + MAX_ATTEMPTS_TOKEN + " intentos (timeout)";
    }

    /**
     * Método de recuperación cuando fallan todos los reintentos para pago.
     */
    @Recover
    public String recuperarProcesarPago(FeignException ex, String token, Map<String, Object> body) {
        System.err.println("Todos los reintentos fallaron para procesar pago con token: " + token);
        System.err.println("Causa: " + ex.getMessage());
        return "Error: No se pudo procesar pago tras " + MAX_ATTEMPTS_PAGO + " intentos";
    }

    @Recover
    public String recuperarProcesarPago(RetryableException ex, String token, Map<String, Object> body) {
        System.err.println("Todos los reintentos fallaron (timeout) para procesar pago con token: " + token);
        System.err.println("Causa: " + ex.getMessage());
        return "Error: No se pudo procesar pago tras " + MAX_ATTEMPTS_PAGO + " intentos (timeout)";
    }
}
