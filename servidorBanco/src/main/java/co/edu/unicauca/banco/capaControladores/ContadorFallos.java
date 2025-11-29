package co.edu.unicauca.banco.capaControladores;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class ContadorFallos {
    private final AtomicInteger contador = new AtomicInteger(0);

    public int siguienteIntento() {
        return contador.incrementAndGet();
    }

    public void resetear() {
        contador.set(0);
    }
}