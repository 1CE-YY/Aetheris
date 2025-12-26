package com.aetheris.rag.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates password complexity requirements.
 *
 * <p>Password must contain:
 *
 * <ul>
 *   <li>At least one letter (a-z, A-Z)
 *   <li>At least one digit (0-9)
 * </ul>
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-26
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordComplexityValidator.class)
@Documented
public @interface PasswordComplexity {

  String message() default "Password must contain both letters and numbers";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
