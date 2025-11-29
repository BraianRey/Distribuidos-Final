/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package co.edu.unicauca.servidorchat.capaFachadaServices.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class MensajeReproduccionDTO {
    private String nickname;
    private int idCancion;
    private Boolean reproduciendo;
}
