package com.vendingcom.machine_service.util.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Valida que un String sea JSON sintácticamente válido (o null/blank, que se consideran ausencia).
 * Se aplica a los campos que terminan en columnas JSONB para devolver un 400 controlado
 * en lugar de un 500 de PostgreSQL cuando el valor no es JSON.
 */
@Documented
@Constraint(validatedBy = ValidJsonValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidJson {

    String message() default "El campo debe contener un JSON válido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
