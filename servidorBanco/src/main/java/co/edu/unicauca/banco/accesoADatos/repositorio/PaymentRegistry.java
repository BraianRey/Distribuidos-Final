package co.edu.unicauca.banco.accesoADatos.repositorio;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import co.edu.unicauca.banco.accesoADatos.entity.OperacionToken;

public class PaymentRegistry {

    private static final String LOG_FILE = "servidorBanco" + File.separator + "payments_log.json";
    private static final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()
            .configure(SerializationFeature.INDENT_OUTPUT, true);

    private static List<Map<String, Object>> readAll() {
        File f = new File(LOG_FILE);
        if (!f.exists()) {
            // Crear Json
            writeAll(new ArrayList<>());
            return new ArrayList<>();
        }
        try {
            return mapper.readValue(f, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public static List<Map<String, Object>> readAllEntries() {
        return readAll();
    }

    private static void writeAll(List<Map<String, Object>> list) {
        try {
            mapper.writeValue(new File(LOG_FILE), list);
        } catch (IOException e) {
            System.err.println("No se pudo escribir el registro de pagos: " + e.getMessage());
        }
    }

    public static void recordGeneration(OperacionToken token) {
        List<Map<String, Object>> all = readAll();
        Map<String, Object> entry = Map.of(
            "tipo", "generacion",
            "token", token.getToken(),
            "nombreCliente", token.getNombreCliente(),
            "costoGeneracion", token.getCostoGeneracion(),
            "fecha", LocalDateTime.now().toString());
        all.add(entry);
        writeAll(all);
    }

    public static void recordPayment(OperacionToken token) {
        List<Map<String, Object>> all = readAll();
        Map<String, Object> entry = Map.of(
            "tipo", "pago",
            "token", token.getToken(),
            "nombreCliente", token.getNombreCliente(),
            "montoRetirado", token.getMontoRetirado(),
            "fecha", LocalDateTime.now().toString());
        all.add(entry);
        writeAll(all);
    }
}
