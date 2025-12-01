package co.edu.unicauca.servidorchat.capaAccesoADatos;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

@Repository
public class RepositorioClientes {
     private final ConcurrentHashMap<String, String> sessionNicknameMap;

     public RepositorioClientes() {
         this.sessionNicknameMap = new ConcurrentHashMap<>();
     }

     public void addSession(String sessionId, String nickname) {
         sessionNicknameMap.put(sessionId, nickname);
     }

     public void removeSession(String sessionId) {
         sessionNicknameMap.remove(sessionId);
     }

     public String getNickname(String sessionId) {
         return sessionNicknameMap.get(sessionId);
     }
}
