package co.edu.unicauca.banco.capaFachadaServices;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import co.edu.unicauca.banco.accesoADatos.entity.OperacionToken;
import co.edu.unicauca.banco.accesoADatos.repositorio.OperacionTokenMemoryRepository;

@Service
public class OpeacionServiceImpl implements OperacionServiceInt{

    @Autowired
    private OperacionTokenMemoryRepository tokenRepo;

    public String generarToken(String nombreCliente) {
        System.out.println("Generando token para cliente " + nombreCliente);
        // costo por token generado (informativo)
        double costo = 10.0;
        String token = UUID.randomUUID().toString();
        OperacionToken opToken = new OperacionToken();
        opToken.setToken(token);
        opToken.setUsado(false);
        opToken.setFechaCreacion(LocalDateTime.now());
        opToken.setCostoGeneracion(costo);
        opToken.setNombreCliente(nombreCliente);
        // Intento atómico de guardar token en repositorio; el repositorio bloquea la generación
        boolean saved = tokenRepo.trySaveGeneration(opToken);
        if (!saved) {
            int paidCount = tokenRepo.countPayments(nombreCliente);
            String msg = "Limite alcanzado para cliente " + nombreCliente + " (" + paidCount + "/5 pagos). No se genera nuevo token.";
            System.out.println("BLOQUEO: " + msg);
            return msg;
        }
        return token;
    }

    public String procesarPago(String token, double monto, String nombreCliente) {
        System.out.println("Procesando pago para cliente " + nombreCliente);

        String respuesta = "";
        Optional<OperacionToken> opToken = tokenRepo.buscarPorId(token);
        if (!opToken.isPresent()) {
            respuesta = "Token no existe";
            System.out.println("RECHAZO PAGO: " + respuesta + " (token=" + token + ", cliente=" + nombreCliente + ")");
            return respuesta;
        }
        OperacionToken t = opToken.get();
        if (t.isUsado()) {
            respuesta = "Token ya fue usado";
            System.out.println("RECHAZO PAGO: " + respuesta + " (token=" + token + ", cliente=" + nombreCliente + ")");
            return respuesta;
        }
        System.out.println("Procesando pago de $" + monto);
        t.setMontoRetirado(monto);
        t.setNombreCliente(nombreCliente);
        boolean registered = this.tokenRepo.tryRegisterPayment(t);
        if (!registered) {
            String msg = "Limite alcanzado para cliente " + nombreCliente + " (" + tokenRepo.countPayments(nombreCliente) + "/5 pagos)";
            System.out.println("RECHAZO PAGO: " + msg);
            return msg;
        }
        // El repositorio ya actualizó la memoria y persiste el pago en el log de forma atómica
        respuesta = "pago exitoso";
        System.out.println("EXITO PAGO: " + respuesta + " (token=" + token + ", cliente=" + nombreCliente + ", monto=$" + monto + ")");
        return respuesta;
    }

    public List<Double> getMontosPagados(String nombreCliente)
    {
        return tokenRepo.getMontosPagados(nombreCliente);
    }
}
