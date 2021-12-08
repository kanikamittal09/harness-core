package io.harness.cvng.beans;

import static io.harness.rule.OwnerRule.KANHAIYA;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashSet;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.reflections.Reflections;

public class DataCollectionRequestTypeTest extends CategoryTest {
  private static final String BASE_PACKAGE = "io.harness.cvng";

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void test() {
    Reflections ref = new Reflections(BASE_PACKAGE);
    final Set<Class<? extends DataCollectionRequest>> resourceClasses = ref.getSubTypesOf(DataCollectionRequest.class);
    Set<String> uniqueJsonTypeNameSet = new HashSet<>();
    for (Class c : resourceClasses) {
      if (c.isAnnotationPresent(JsonTypeName.class)) {
        String s = c.getAnnotation(JsonTypeName.class).toString();
        if (uniqueJsonTypeNameSet.contains(s)) {
          Assert.fail("Multiple classes exists with same Json type Annotation: " + s);
        }
        uniqueJsonTypeNameSet.add(s);
      }
    }
  }
}