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
        int val = contador.incrementAndGet();
        System.out.println("[CFallo] siguienteIntento -> " + val + " (thread=" + Thread.currentThread().getName() + ")");
        return val;
    }

    public void resetear() {
        contador.set(0);
        System.out.println("[CFallo] resetear -> contador a 0");
    }

    /**
     * Indica si, dado el número de intento actual, debemos simular fallo
     * durante la secuencia inicial de las primeras operaciones.
     */
    public boolean shouldSimulateInitialFailure(int intento) {
        boolean should = initialOperationsRemaining.get() > 0 && intento <= initialFailureCountPerOperation;
        System.out.println("[CFallos] shouldSimulateInitialFailure? intento=" + intento + ", remainingOps=" + initialOperationsRemaining.get() + ", perOpFailCount=" + initialFailureCountPerOperation + " -> " + should);
        return should;
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
        System.out.println("[CFallos] markInitialSequenceDone -> remaining=" + Math.max(0, remaining) + ", contador reseteado a 0");
    }
}