package com.example.todoapp.dto;

/**
 * DTO returned with HTTP 400 when a request field fails validation.
 */
public class ValidationErrorDto {

    private final String field;
    private final String message;

    public ValidationErrorDto(String field, String message) {
        this.field = field;
        this.message = message;
    }

    public String getField() {
        return field;
    }

    public String getMessage() {
        return message;
    }
}
