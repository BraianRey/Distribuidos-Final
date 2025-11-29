package co.edu.unicauca.banco.accesoADatos.repositorio;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import co.edu.unicauca.banco.accesoADatos.entity.OperacionToken;

@Component
public class OperacionTokenMemoryRepository {

    private final Map<String, OperacionToken> almacenamiento = new ConcurrentHashMap<>();

    public void guardar(OperacionToken token) {
        almacenamiento.put(token.getToken(), token);
    }

    public Optional<OperacionToken> buscarPorId(String token) {
        return Optional.ofNullable(almacenamiento.get(token));
    }

    public void actualizar(OperacionToken token) {
        almacenamiento.put(token.getToken(), token);
    }

    public List<Double> getMontosRetirados(int idCliente) {
        List<Double> montos = new ArrayList();
        for (OperacionToken token : almacenamiento.values()) {
            if (token.isUsado() && token.getIdCliente() == idCliente) {
                montos.add(token.getMontoRetirado());
            }
        }
        return montos;
    }
}

