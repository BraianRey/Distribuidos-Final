package co.edu.unicauca.servidorchat.capaControladores;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import co.edu.unicauca.servidorchat.capaFachadaServices.DTO.MensajePrivadoDTO;
import co.edu.unicauca.servidorchat.capaFachadaServices.DTO.MensajePublicoDTO;
import co.edu.unicauca.servidorchat.capaFachadaServices.DTO.MensajeReproduccionDTO;

@Controller
public class ChatController {

  @Autowired
  private SimpMessagingTemplate simpMessagingTemplate;

  @MessageMapping("/enviarMensajePublico")
  @SendTo("/chatGrupal/salaChatPublica/")
  public MensajePublicoDTO enviarMensajeGrupal(MensajePublicoDTO mensaje) {
    mensaje.setContenido(mensaje.getNickname() + ":" + mensaje.getContenido());
    mensaje.setFechaGeneracion(new Date());
    return mensaje;
  }

  @MessageMapping("/enviarMensajePrivado/")
  public void enviarMensajePrivado(MensajePrivadoDTO mensaje) {
    System.out.println("🔵 Reacción recibida en servidor: " + mensaje);
    simpMessagingTemplate.convertAndSend("/chatPrivado/" + mensaje.getIdCancion(), mensaje);
  }

  @MessageMapping("/avisarPlay/")
  public void avisarReproduccionCancion(MensajeReproduccionDTO estado) {
    simpMessagingTemplate.convertAndSend("/avisarPlay/" + estado.getIdCancion(), estado);
  }
}