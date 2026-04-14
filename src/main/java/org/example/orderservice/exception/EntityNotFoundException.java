package org.example.orderservice.exception;

public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String message) {
        super(message);
    }

    public EntityNotFoundException(String entityName, Object id) {
        super("%s not found with id: %s".formatted(entityName, id));
    }
}
