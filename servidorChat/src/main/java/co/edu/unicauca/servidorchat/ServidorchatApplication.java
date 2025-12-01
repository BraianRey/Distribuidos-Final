package co.edu.unicauca.servidorchat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
@EnableFeignClients
public class ServidorchatApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServidorchatApplication.class, args);
	}

}
