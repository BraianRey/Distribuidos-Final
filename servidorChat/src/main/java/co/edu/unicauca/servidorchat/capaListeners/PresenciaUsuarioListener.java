package co.edu.unicauca.servidorchat.capaListeners;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import co.edu.unicauca.servidorchat.capaAccesoADatos.RepositorioClientes;

@Component
public class PresenciaUsuarioListener {

    @Autowired
    private RepositorioClientes repositorioClientes;
    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;
   
    @EventListener
    public void handleSessionConnected(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String nickname = accessor.getFirstNativeHeader("nickname");

        if (nickname != null) {
            System.out.println("El usuario " + nickname + " se ha conectado con la sesión: " + sessionId);
            this.repositorioClientes.addSession(sessionId, nickname);
            simpMessagingTemplate.convertAndSend("/chatGrupal/notificaciones",
                    nickname + " se ha conectado.");
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        String nickname = this.repositorioClientes.getNickname(sessionId);
        this.repositorioClientes.removeSession(sessionId);
        if (nickname != null) {
            System.out.println("El usuario " + nickname + " se ha desconectado de la sesión: " + sessionId);
            simpMessagingTemplate.convertAndSend("/chatGrupal/notificaciones",
                    nickname + " se ha desconectado.");
        } 
    }
}