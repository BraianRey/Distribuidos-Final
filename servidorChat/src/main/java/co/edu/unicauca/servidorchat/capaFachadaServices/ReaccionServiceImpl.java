package co.edu.unicauca.servidorchat.capaFachadaServices;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import co.edu.unicauca.servidorchat.capaFachadaServices.DTO.MensajePrivadoDTO;
import co.edu.unicauca.servidorchat.servicios.BancoClient;

import java.util.Map;

/**
 * Servicio que procesa reacciones de usuarios.
 * Orquesta comunicación con banco usando BancoClient (con reintentos automáticos).
 * Sigue el patrón de clienterest/OperacionesClienteImpl.
 */
@Service
public class ReaccionServiceImpl implements ReaccionServiceInt {

    @Autowired
    private BancoClient bancoClient;

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    private static final double MONTO = 10.0;

    @Override
    public void procesarReaccion(MensajePrivadoDTO mensaje) {
        String nombreCliente = mensaje.getNicknameOrigen();

        System.out.println("📥 Reacción recibida: cliente=" + nombreCliente + 
                         ", cancion=" + mensaje.getIdCancion() + 
                         ", reaccion=" + mensaje.getReaction());

        // 1) Generar token (con reintentos automáticos vía @Retryable)
        String token = null;
        try {
            token = bancoClient.generarToken(nombreCliente);
        } catch (RestClientException ex) {
            System.out.println("❌ Error generando token para " + nombreCliente + ": " + ex.getMessage());
            notificarError(mensaje, "Error al generar token: " + ex.getMessage());
            return;
        } catch (Exception ex) {
            System.out.println("❌ Error inesperado generando token para " + nombreCliente + ": " + ex.getMessage());
            notificarError(mensaje, "Error al generar token");
            return;
        }

        if (token == null || token.isEmpty()) {
            System.out.println("❌ Token nulo o vacío para " + nombreCliente);
            notificarError(mensaje, "No se pudo obtener token");
            return;
        }

        if (token.contains("Error:") || token.contains("Limite alcanzado")) {
            System.out.println("🚫 Banco rechazó: " + token);
            notificarError(mensaje, token);
            return;
        }

        System.out.println("✓ Token generado exitosamente: " + token);

        // 2) Procesar pago (con reintentos automáticos vía @Retryable)
        procesarPago(token, nombreCliente, mensaje);
    }

    private void procesarPago(String token, String nombreCliente, MensajePrivadoDTO mensaje) {
        Map<String, Object> bodyPago = Map.of(
            "nombreCliente", nombreCliente,
            "monto", MONTO
        );

        try {
            String respuesta = bancoClient.procesarPago(token, bodyPago);
            
            String resp = respuesta == null ? "" : respuesta.trim().toLowerCase();
            
            if (resp.contains("pago exitoso") || resp.contains("éxito")) {
                System.out.println("✓ Pago exitoso para " + nombreCliente);
                // Broadcast del mensaje vía WebSocket
                simpMessagingTemplate.convertAndSend("/chatPrivado/" + mensaje.getIdCancion(), mensaje);
            } else if (resp.contains("error") || resp.contains("limite alcanzado") || resp.contains("token")) {
                System.out.println("🚫 Banco rechazó pago: " + respuesta);
                notificarError(mensaje, "Pago rechazado: " + respuesta);
            } else {
                System.out.println("⚠️ Respuesta inesperada: " + respuesta);
                notificarError(mensaje, "Respuesta inesperada del banco: " + respuesta);
            }
        } catch (RestClientException ex) {
            System.out.println("❌ Error procesando pago para " + nombreCliente + ": " + ex.getMessage());
            notificarError(mensaje, "Error al procesar pago: " + ex.getMessage());
        } catch (Exception ex) {
            System.out.println("❌ Error inesperado procesando pago para " + nombreCliente + ": " + ex.getMessage());
            notificarError(mensaje, "Error inesperado al procesar pago");
        }
    }

    private void notificarError(MensajePrivadoDTO mensaje, String contenido) {
        MensajePrivadoDTO error = new MensajePrivadoDTO();
        error.setNicknameOrigen("server");
        error.setIdCancion(mensaje.getIdCancion());
        error.setContenido(contenido);
        simpMessagingTemplate.convertAndSend("/chatPrivado/" + mensaje.getIdCancion(), error);
    }
}
