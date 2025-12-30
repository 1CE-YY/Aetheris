package com.aetheris.rag.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * {@link PasswordComplexity} 注解的验证器。
 *
 * <p>验证密码是否同时包含字母和数字。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-26
 */
public class PasswordComplexityValidator implements ConstraintValidator<PasswordComplexity, String> {

  @Override
  public boolean isValid(String password, ConstraintValidatorContext context) {
    if (password == null || password.isEmpty()) {
      return true; // 让 @NotBlank 处理 null/empty 验证
    }

    boolean hasLetter = password.matches(".*[a-zA-Z].*");
    boolean hasDigit = password.matches(".*\\d.*");

    return hasLetter && hasDigit;
  }
}
