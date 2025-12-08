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
    private static final int MAX_PAYMENTS = 5;

    public void guardar(OperacionToken token) {
        synchronized (this) {
            almacenamiento.put(token.getToken(), token);
        }
        System.out.println("Guardado token=" + token.getToken() + " cliente=" + token.getNombreCliente() + " fecha=" + token.getFechaCreacion());
    }

    public Optional<OperacionToken> buscarPorId(String token) {
        OperacionToken t = almacenamiento.get(token);
        System.out.println("buscarPorId token=" + token + " -> " + (t != null ? "ENCONTRADO" : "NO_ENCONTRADO"));
        return Optional.ofNullable(t);
    }

    public void actualizar(OperacionToken token) {
        synchronized (this) {
            almacenamiento.put(token.getToken(), token);
        }
        System.out.println("actualizar token=" + token.getToken() + " usado=" + token.isUsado() + " monto=" + token.getMontoRetirado());
    }

    /**
     * Intento atómico de registrar un pago en memoria: verifica el limite y, si es permitido,
     * marca el token como usado y lo guarda.
     * @param token token a registrar (debe tener token.id y nombreCliente)
     * @return true si el pago fue registrado en memoria, false si el limite ya se alcanzó
     */
    public boolean tryRegisterPayment(OperacionToken token) {
        return tryRegisterPayment(token, true);
    }

    public boolean tryRegisterPayment(OperacionToken token, boolean persist) {
        synchronized (this) {
            int paidCount = countPayments(token.getNombreCliente());
            OperacionToken existing = almacenamiento.get(token.getToken());
            boolean willIncrease = (existing == null) || !existing.isUsado();
            if (willIncrease && paidCount >= MAX_PAYMENTS) {
                return false;
            }
            token.setUsado(true);
            almacenamiento.put(token.getToken(), token);
            if (persist) {
                try {
                    PaymentRegistry.recordPayment(token);
                } catch (Exception ex) {
                    System.err.println("No se pudo persistir pago en log: " + ex.getMessage());
                }
            }
            return true;
        }
    }

    /**
     * Intento atómico de guardar un token generado: si el cliente ya alcanzó el limite
     * de pagos registrados, no permite generar un nuevo token.
     * @param token token a guardar (no usado)
     * @return true si el token fue guardado, false si el cliente ya alcanzó el limite
     */
    public boolean trySaveGeneration(OperacionToken token) {
        return trySaveGeneration(token, true);
    }

    public boolean trySaveGeneration(OperacionToken token, boolean persist) {
        synchronized (this) {
            int paidCount = countPayments(token.getNombreCliente());
            if (paidCount >= MAX_PAYMENTS) {
                return false;
            }
            almacenamiento.put(token.getToken(), token);
            if (persist) {
                try {
                    PaymentRegistry.recordGeneration(token);
                } catch (Exception ex) {
                    System.err.println("No se pudo persistir generacion en log: " + ex.getMessage());
                }
            }
            return true;
        }
    }

    public List<Double> getMontosPagados(String nombreCliente) {
        List<Double> montos = new ArrayList<>();
        for (OperacionToken token : almacenamiento.values()) {
            if (token.isUsado() && nombreCliente != null && nombreCliente.equals(token.getNombreCliente())) {
                montos.add(token.getMontoRetirado());
            }
        }
        System.out.println("getMontosPagados nombreCliente=" + nombreCliente + " -> " + montos.size() + " registros");
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
                        // Intentar guardar la generación respetando el límite de pagos por cliente
                        boolean saved = trySaveGeneration(t, false);
                        if (!saved) {
                            System.out.println("OperacionTokenMemoryRepository.initFromLog: salto generacion para cliente " + cliente + " porque ya alcanzó el limite de " + MAX_PAYMENTS + " pagos");
                        }
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
                    t.setNombreCliente(cliente);
                    // Intentar registrar el pago en memoria de forma atómica; si no es posible (limite), se omite
                    boolean registered = tryRegisterPayment(t, false);
                    if (!registered) {
                        System.out.println("OperacionTokenMemoryRepository.initFromLog: salto pago para cliente " + cliente + " porque ya alcanzó el limite de " + MAX_PAYMENTS + " pagos");
                        continue;
                    }
                }
            }
            System.out.println("OperacionTokenMemoryRepository: cargado " + almacenamiento.size() + " tokens desde payments_log.json");
        } catch (Exception ex) {
            System.err.println("No se pudo inicializar repositorio desde log: " + ex.getMessage());
        }
    }
}

