package com.rapports.moteur.exceptions;

import java.util.List;

public class ValidationException extends RuntimeException {
    private final List<String> errors;

    public ValidationException(List<String> errors) {
        super("Validation echouee: " + String.join(", ", errors));
        this.errors = errors;
    }

    public ValidationException(String message){
        super(message);
        this.errors = List.of(message);
    }

    public List<String> getErrors() { return errors; }
}
