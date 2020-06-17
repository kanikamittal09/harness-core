package io.harness.data.validator;

import static io.harness.data.validator.EntityIdentifierValidator.NOT_ALLOWED_WORDS;
import static io.harness.rule.OwnerRule.VIKAS;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import lombok.Builder;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Random;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

public class EntityIdentifierValidatorTest {
  private Validator validator;

  @Builder
  static class EntityIdentifierValidatorTestStructure {
    @EntityIdentifier String identifier;
  }

  @Before
  public void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testEntityIdentifierValidator_For_NotAllowedWords() {
    int violationCount =
        NOT_ALLOWED_WORDS.stream()
            .mapToInt(identifier
                -> validator.validate(EntityIdentifierValidatorTestStructure.builder().identifier(identifier).build())
                       .size())
            .sum();

    assertEquals(
        "Violation count should be same as NOT_ALLOWED_WORDS set size", NOT_ALLOWED_WORDS.size(), violationCount);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testEntityIdentifierValidator_For_NullValue_And_EmptyString() {
    assertEquals("Null identifier should not be allowed", 1,
        validator.validate(EntityIdentifierValidatorTestStructure.builder().build()).size());
    assertEquals("Null identifier should not be allowed", 1,
        validator.validate(EntityIdentifierValidatorTestStructure.builder().identifier(" ").build()).size());
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testEntityIdentifierValidator() {
    assertEquals("Null identifier should not be allowed", 1,
        validator.validate(EntityIdentifierValidatorTestStructure.builder().build()).size());

    EntityIdentifierValidator entityIdentifierValidator = new EntityIdentifierValidator();
    for (int i = 0; i < 5000; i++) {
      String identifier = generateRandomAsciiString();
      int violationsCount =
          validator.validate(EntityIdentifierValidatorTestStructure.builder().identifier(identifier).build()).size();
      if (isValidEntityIdentifier(identifier, entityIdentifierValidator)) {
        assertEquals("identifier : " + identifier, 0, violationsCount);
      } else {
        assertTrue("identifier : " + identifier, violationsCount > 0);
      }
    }
  }

  private static String generateRandomAsciiString() {
    String random = RandomStringUtils.randomAscii(100);
    return random.substring(0, new Random().nextInt(100));
  }

  private static boolean isValidEntityIdentifier(
      String identifier, EntityIdentifierValidator entityIdentifierValidator) {
    if (isBlank(identifier)) {
      return false;
    }

    return entityIdentifierValidator.matchesIdentifierPattern(identifier)
        && entityIdentifierValidator.hasAllowedWords(identifier);
  }

  private static boolean isLetterOrDigit(char c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
  }
}