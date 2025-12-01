package co.edu.unicauca.servidorchat.capaConfiguracionWebSocket;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplateBuilder()
            .setConnectTimeout(Duration.ofSeconds(3))   // connectTimeout (igual que FeignConfig)
            .setReadTimeout(Duration.ofSeconds(3))      // readTimeout (igual que FeignConfig)
            .build();
    }
}
