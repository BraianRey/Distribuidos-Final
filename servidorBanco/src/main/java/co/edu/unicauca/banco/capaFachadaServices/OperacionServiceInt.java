package co.edu.unicauca.banco.capaFachadaServices;

import java.util.List;

public interface OperacionServiceInt {
    public String generarToken();  
    public String procesarRetiro(String token, double monto, int idCliente);
    public List<Double> getMontosRetirados(int idCliente);
}
