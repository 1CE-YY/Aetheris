package com.aetheris.rag.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for {@link PasswordComplexity} annotation.
 *
 * <p>Validates that password contains both letters and numbers.
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-26
 */
public class PasswordComplexityValidator implements ConstraintValidator<PasswordComplexity, String> {

  @Override
  public boolean isValid(String password, ConstraintValidatorContext context) {
    if (password == null || password.isEmpty()) {
      return true; // Let @NotBlank handle null/empty validation
    }

    boolean hasLetter = password.matches(".*[a-zA-Z].*");
    boolean hasDigit = password.matches(".*\\d.*");

    return hasLetter && hasDigit;
  }
}
