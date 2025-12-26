package com.aetheris.rag.validation;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PasswordComplexity} validation.
 *
 * @author Aetheris Team
 * @version 1.0.0
 */
@DisplayName("Password Complexity Validation Tests")
class PasswordComplexityValidatorTest {

  private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
  private final Validator validator = factory.getValidator();

  static class TestModel {
    @PasswordComplexity String password;
  }

  @Test
  @DisplayName("Valid password with letters and numbers should pass")
  void testValidPassword() {
    TestModel model = new TestModel();
    model.password = "password123";

    Set<ConstraintViolation<TestModel>> violations = validator.validate(model);
    assertTrue(violations.isEmpty(), "Valid password should have no violations");
  }

  @Test
  @DisplayName("Valid password with mixed case letters and numbers should pass")
  void testValidPasswordMixedCase() {
    TestModel model = new TestModel();
    model.password = "Pass123Word";

    Set<ConstraintViolation<TestModel>> violations = validator.validate(model);
    assertTrue(violations.isEmpty(), "Valid password should have no violations");
  }

  @Test
  @DisplayName("Password with only letters should fail")
  void testPasswordOnlyLetters() {
    TestModel model = new TestModel();
    model.password = "password";

    Set<ConstraintViolation<TestModel>> violations = validator.validate(model);
    assertFalse(violations.isEmpty(), "Password with only letters should have violations");
    assertEquals(
        "Password must contain both letters and numbers",
        violations.iterator().next().getMessage());
  }

  @Test
  @DisplayName("Password with only numbers should fail")
  void testPasswordOnlyNumbers() {
    TestModel model = new TestModel();
    model.password = "12345678";

    Set<ConstraintViolation<TestModel>> violations = validator.validate(model);
    assertFalse(violations.isEmpty(), "Password with only numbers should have violations");
    assertEquals(
        "Password must contain both letters and numbers",
        violations.iterator().next().getMessage());
  }

  @Test
  @DisplayName("Null password should pass (handled by @NotBlank)")
  void testNullPassword() {
    TestModel model = new TestModel();
    model.password = null;

    Set<ConstraintViolation<TestModel>> violations = validator.validate(model);
    assertTrue(violations.isEmpty(), "Null password should pass (handled by @NotBlank)");
  }

  @Test
  @DisplayName("Empty password should pass (handled by @NotBlank)")
  void testEmptyPassword() {
    TestModel model = new TestModel();
    model.password = "";

    Set<ConstraintViolation<TestModel>> violations = validator.validate(model);
    assertTrue(violations.isEmpty(), "Empty password should pass (handled by @NotBlank)");
  }

  @Test
  @DisplayName("Password with letters and numbers at the end should pass")
  void testValidPasswordNumbersAtEnd() {
    TestModel model = new TestModel();
    model.password = "password123";

    Set<ConstraintViolation<TestModel>> violations = validator.validate(model);
    assertTrue(violations.isEmpty(), "Valid password should have no violations");
  }

  @Test
  @DisplayName("Password with letters and numbers at the beginning should pass")
  void testValidPasswordNumbersAtBeginning() {
    TestModel model = new TestModel();
    model.password = "123password";

    Set<ConstraintViolation<TestModel>> violations = validator.validate(model);
    assertTrue(violations.isEmpty(), "Valid password should have no violations");
  }
}
