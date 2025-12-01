package co.edu.unicauca.banco.capaFachadaServices;

import java.util.List;

public interface OperacionServiceInt {
    public String generarToken(String nombreCliente);  
    public String procesarPago(String token, double monto, String nombreCliente);
    public List<Double> getMontosPagados(String nombreCliente);
}
