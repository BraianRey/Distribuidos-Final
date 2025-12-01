package co.edu.unicauca.banco.capaFachadaServices;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import co.edu.unicauca.banco.accesoADatos.entity.OperacionToken;
import co.edu.unicauca.banco.accesoADatos.repositorio.OperacionTokenMemoryRepository;
import co.edu.unicauca.banco.accesoADatos.repositorio.PaymentRegistry;

@Service
public class OpeacionServiceImpl implements OperacionServiceInt{

    @Autowired
    private OperacionTokenMemoryRepository tokenRepo;

    private static final int MAX_PAYMENTS = 5;

    public String generarToken(String nombreCliente) {
        System.out.println("Generando token para cliente " + nombreCliente);
        // Check if client already reached payment limit
        int paidCount = tokenRepo.countPayments(nombreCliente);
        if (paidCount >= MAX_PAYMENTS) {
            String msg = "Limite alcanzado para cliente " + nombreCliente + " (" + paidCount + "/" + MAX_PAYMENTS + " pagos). No se genera nuevo token.";
            System.out.println("BLOQUEO: " + msg);
            return msg;
        }
        // costo por token generado (informativo)
        double costo = 10.0;
        String token = UUID.randomUUID().toString();
        OperacionToken opToken = new OperacionToken();
        opToken.setToken(token);
        opToken.setUsado(false);
        opToken.setFechaCreacion(LocalDateTime.now());
        opToken.setCostoGeneracion(costo);
        opToken.setNombreCliente(nombreCliente);
        tokenRepo.guardar(opToken);
        // registrar generación en JSON
        PaymentRegistry.recordGeneration(opToken);
        return token;
    }

    public String procesarPago(String token, double monto, String nombreCliente) {
        System.out.println("Procesando pago para cliente " + nombreCliente);

        int paidCount = tokenRepo.countPayments(nombreCliente);
        if (paidCount >= MAX_PAYMENTS) {
            String msg = "Limite alcanzado para cliente " + nombreCliente + " (" + paidCount + "/" + MAX_PAYMENTS + " pagos)";
            System.out.println("RECHAZO PAGO: " + msg);
            return msg;
        }
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
        t.setUsado(true);
        t.setMontoRetirado(monto);
        t.setNombreCliente(nombreCliente);
        this.tokenRepo.actualizar(t);
        // registrar pago en JSON
        PaymentRegistry.recordPayment(t);
        respuesta = "pago exitoso";
        System.out.println("EXITO PAGO: " + respuesta + " (token=" + token + ", cliente=" + nombreCliente + ", monto=$" + monto + ")");
        return respuesta;
    }

    public List<Double> getMontosPagados(String nombreCliente)
    {
        return tokenRepo.getMontosPagados(nombreCliente);
    }
}
