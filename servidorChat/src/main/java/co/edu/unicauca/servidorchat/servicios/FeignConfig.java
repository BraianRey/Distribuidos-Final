package co.edu.unicauca.servidorchat.servicios;

import feign.Request;
import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

@Configuration
public class FeignConfig {
    
    /**
     * Configuración de timeouts para Feign
     * connectTimeout: tiempo máximo para establecer conexión
     * readTimeout: tiempo máximo para leer la respuesta
     */
    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
            Duration.ofSeconds(3),  // connectTimeout
            Duration.ofSeconds(3),  // readTimeout
            false
        );
    }
    
    /**
     * Retryer a nivel de cliente Feign
     * Complementa @Retryable de Spring Retry para reintentos más granulares
     */
    @Bean
    public Retryer retryer() {
        // Desactivar los reintentos internos de Feign para dejar el control
        return Retryer.NEVER_RETRY;
    }
}
