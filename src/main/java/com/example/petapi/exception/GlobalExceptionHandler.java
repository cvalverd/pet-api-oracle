package com.example.petapi.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PetNotFoundException.class)
    public ResponseEntity<Object> handleNotFound(PetNotFoundException ex) {
        return respuesta(HttpStatus.NOT_FOUND, Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        return respuesta(HttpStatus.BAD_REQUEST, Map.of("errors", errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneralException(Exception ex) {
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, Map.of("message", "Ocurrio un error inesperado"));
    }

    private ResponseEntity<Object> respuesta(HttpStatus status, Map<String, Object> cuerpo) {
        Map<String, Object> response = new HashMap<>(cuerpo);
        response.put("timestamp", LocalDateTime.now());
        response.put("status", status.value());
        return new ResponseEntity<>(response, status);
    }
}
