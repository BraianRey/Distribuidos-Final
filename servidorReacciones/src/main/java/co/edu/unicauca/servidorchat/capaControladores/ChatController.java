package co.edu.unicauca.servidorchat.capaControladores;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import co.edu.unicauca.servidorchat.capaFachadaServices.DTO.MensajePublicoDTO;
import co.edu.unicauca.servidorchat.capaFachadaServices.DTO.MensajePrivadoDTO;
import co.edu.unicauca.servidorchat.capaFachadaServices.DTO.MensajeReproduccionDTO;

@Controller
public class ChatController {

  @Autowired
  private SimpMessagingTemplate simpMessagingTemplate;
  
  // Mensajes grupales: el cliente envía a /apiChat/enviarGrupal y todos suscritos a /chatGrupal/sala reciben el mensaje
  @MessageMapping("/enviarMensajePublico")
  @SendTo("/chatGrupal/salaChatPublica")
  public MensajePublicoDTO enviarMensajeGrupal(MensajePublicoDTO mensaje) {
    mensaje.setContenido(mensaje.getNickname()+":"+mensaje.getContenido()); 
    mensaje.setFechaGeneracion(new Date()); 
    return mensaje; // reenviamos el mensaje a todos suscritos a /chatGrupal/sala
  }

  // Mensajes privados: los clientes envían a /apiChat/enviarMensajePrivado y se retransmite a todos
  // los usuarios suscritos a la canción indicada en /chatPrivado/{idCancion}
  @MessageMapping("/enviarMensajePrivado/")
  public void enviarMensajePrivado(MensajePrivadoDTO mensaje) {
    if (mensaje.getContenido() == null && mensaje.getReaction() != null) {
      mensaje.setContenido(mensaje.getReaction());
    }

    Integer idCancion = mensaje.getIdCancion();
    if (idCancion == null) {
      return; // no hay canción que notificar
    }

    simpMessagingTemplate.convertAndSend("/chatPrivado/" + idCancion, mensaje);
  }

  @MessageMapping("/avisarPlay/")
  public void avisarReproduccionCancion(MensajeReproduccionDTO estado) {
    if (estado.getIdCancion() == null) {
      return;
    }
    simpMessagingTemplate.convertAndSend("/avisarPlay/" + estado.getIdCancion(), estado);
  }

 
}

