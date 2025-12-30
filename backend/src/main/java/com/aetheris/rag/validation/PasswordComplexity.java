package com.aetheris.rag.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 验证密码复杂性要求。
 *
 * <p>密码必须包含：
 *
 * <ul>
 *   <li>至少一个字母（a-z、A-Z）
 *   <li>至少一个数字（0-9）
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
