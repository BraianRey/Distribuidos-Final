package co.edu.unicauca.banco.capaControladoraExcepciones;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException ex) {
        Map<String, String> errorBody = new HashMap<>();
        errorBody.put("error", ex.getMessage());

        if (ex.getMessage().contains("ya fue usado")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody); // 400
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody); // 500
    }
}
