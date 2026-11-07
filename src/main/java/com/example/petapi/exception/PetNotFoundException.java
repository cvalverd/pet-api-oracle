package com.example.petapi.exception;

public class PetNotFoundException extends RuntimeException {
    public PetNotFoundException(Long id) {
        super("No existe la mascota con id " + id);
    }
}
