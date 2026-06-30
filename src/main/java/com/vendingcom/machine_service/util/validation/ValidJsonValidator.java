package com.vendingcom.machine_service.util.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Verifica que el valor sea JSON válido intentando parsearlo con Jackson.
 * Un valor null o en blanco se considera válido (la obligatoriedad la cubre @NotNull/@NotBlank).
 */
public class ValidJsonValidator implements ConstraintValidator<ValidJson, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        try {
            OBJECT_MAPPER.readTree(value);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }
}
