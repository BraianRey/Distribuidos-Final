package co.edu.unicauca.banco.capaFachadaServices.DTOS;

import lombok.Data;

@Data
public class OperacionRetiroDTO {    
    private double monto;
    private int idCliente;
}