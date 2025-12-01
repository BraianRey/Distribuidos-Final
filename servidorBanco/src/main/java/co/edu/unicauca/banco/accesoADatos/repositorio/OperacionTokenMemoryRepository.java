package co.edu.unicauca.banco.accesoADatos.repositorio;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.PostConstruct;

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

    public List<Double> getMontosPagados(String nombreCliente) {
        List<Double> montos = new ArrayList<>();
        for (OperacionToken token : almacenamiento.values()) {
            if (token.isUsado() && nombreCliente != null && nombreCliente.equals(token.getNombreCliente())) {
                montos.add(token.getMontoRetirado());
            }
        }
        return montos;
    }

    public int countPayments(String nombreCliente) {
        int count = 0;
        for (OperacionToken token : almacenamiento.values()) {
            if (token.isUsado() && nombreCliente != null && nombreCliente.equals(token.getNombreCliente())) {
                count++;
            }
        }
        return count;
    }

    public double getTotalCharged(String nombreCliente) {
        double total = 0.0;
        for (OperacionToken token : almacenamiento.values()) {
            if (nombreCliente != null && nombreCliente.equals(token.getNombreCliente())) {
                total += token.getCostoGeneracion();
                if (token.isUsado()) {
                    total += token.getMontoRetirado();
                }
            }
        }
        return total;
    }

    @PostConstruct
    public void initFromLog() {
        try {
            List<Map<String, Object>> entries = PaymentRegistry.readAllEntries();
            if (entries == null || entries.isEmpty()) return;
            // Process entries in order: create or update tokens
            for (Map<String, Object> e : entries) {
                Object tipoObj = e.get("tipo");
                if (tipoObj == null) continue;
                String tipo = tipoObj.toString();
                String tokenId = e.getOrDefault("token", "").toString();
                String cliente = e.getOrDefault("nombreCliente", null) == null ? null : e.get("nombreCliente").toString();
                if (tipo.equalsIgnoreCase("generacion")) {
                    OperacionToken t = almacenamiento.get(tokenId);
                    if (t == null) {
                        t = new OperacionToken();
                        t.setToken(tokenId);
                        t.setNombreCliente(cliente);
                        Object costo = e.get("costoGeneracion");
                        if (costo instanceof Number) {
                            t.setCostoGeneracion(((Number) costo).doubleValue());
                        } else {
                            try { t.setCostoGeneracion(Double.parseDouble(costo.toString())); } catch (Exception ex) { t.setCostoGeneracion(0.0); }
                        }
                        t.setUsado(false);
                        almacenamiento.put(tokenId, t);
                    }
                } else if (tipo.equalsIgnoreCase("pago")) {
                    OperacionToken t = almacenamiento.get(tokenId);
                    if (t == null) {
                        t = new OperacionToken();
                        t.setToken(tokenId);
                        t.setNombreCliente(cliente);
                        t.setCostoGeneracion(0.0);
                        almacenamiento.put(tokenId, t);
                    }
                    Object monto = e.get("montoRetirado");
                    if (monto instanceof Number) {
                        t.setMontoRetirado(((Number) monto).doubleValue());
                    } else {
                        try { t.setMontoRetirado(Double.parseDouble(monto.toString())); } catch (Exception ex) { t.setMontoRetirado(0.0); }
                    }
                    t.setUsado(true);
                    almacenamiento.put(tokenId, t);
                }
            }
            System.out.println("OperacionTokenMemoryRepository: cargado " + almacenamiento.size() + " tokens desde payments_log.json");
        } catch (Exception ex) {
            System.err.println("No se pudo inicializar repositorio desde log: " + ex.getMessage());
        }
    }
}

