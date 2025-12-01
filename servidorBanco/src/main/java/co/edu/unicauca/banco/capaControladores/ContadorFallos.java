package co.edu.unicauca.banco.capaControladores;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class ContadorFallos {
    private final AtomicInteger contador = new AtomicInteger(0);
    // Número de operaciones iniciales que deben someterse a la simulación de fallos
    private final AtomicInteger initialOperationsRemaining = new AtomicInteger(2);
    // Número de fallos (intentos) a simular por cada una de las operaciones iniciales
    private final int initialFailureCountPerOperation = 4;

    public int siguienteIntento() {
        return contador.incrementAndGet();
    }

    public void resetear() {
        contador.set(0);
    }

    /**
     * Indica si, dado el número de intento actual, debemos simular fallo
     * durante la secuencia inicial de las primeras operaciones.
     */
    public boolean shouldSimulateInitialFailure(int intento) {
        return initialOperationsRemaining.get() > 0 && intento <= initialFailureCountPerOperation;
    }

    /**
     * Marca que una de las operaciones iniciales que estaba siendo simulada
     * ya fue completada con éxito. Decrementa el contador de operaciones restantes
     * y reinicia el contador de intentos para la siguiente operación.
     */
    public void markInitialSequenceDone() {
        int remaining = initialOperationsRemaining.decrementAndGet();
        if (remaining < 0) {
            initialOperationsRemaining.set(0);
        }
        contador.set(0);
    }
}