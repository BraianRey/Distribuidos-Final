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

    public String generarToken() {
        System.out.println("Generando token");
        String token = UUID.randomUUID().toString();
        OperacionToken opToken = new OperacionToken();
        opToken.setToken(token);
        opToken.setUsado(false);
        opToken.setFechaCreacion(LocalDateTime.now());
        tokenRepo.guardar(opToken);
        return token;
    }

    public String procesarRetiro(String token, double monto, int idCliente) {
        System.out.println("Procesando retiro");
        String respuesta="";
        Optional<OperacionToken> opToken= tokenRepo.buscarPorId(token);
        if (opToken == null) {
            respuesta="Token no existe"; 
        } else if (opToken.get().isUsado()) {
            respuesta="Token ya fue usado";
        }
        else{
             System.out.println("Retirando $" + monto);
            opToken.get().setUsado(true);
            opToken.get().setMontoRetirado(monto);
            opToken.get().setIdCliente(idCliente);
            this.tokenRepo.actualizar(opToken.get());
             respuesta="retiro exitoso";
        }
        return respuesta;
       
    }

    public List<Double> getMontosRetirados(int idCliente)
    {
        return tokenRepo.getMontosRetirados(idCliente);
    }
}
