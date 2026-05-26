package com.centricorp.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción lanzada cuando un recurso no es encontrado en la BD.
 * Produce un HTTP 404 Not Found automáticamente vía @ResponseStatus.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String recurso, Object id) {
        super(String.format("%s con id '%s' no fue encontrado.", recurso, id));
    }

    public ResourceNotFoundException(String mensaje) {
        super(mensaje);
    }
}
