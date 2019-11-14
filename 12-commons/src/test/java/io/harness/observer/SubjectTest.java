package io.harness.observer;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SubjectTest extends CategoryTest {
  private static final String OBSERVER_KEY = "observer";
  private static final String OBSERVER_1_KEY = "observer1";
  private Subject<String> subject;

  private Subject.Approver<String, String, Rejection> testFunc = (s, t) -> s.equals(t) ? null : (Rejection) () -> s;

  @Before
  public void initialize() {
    subject = new Subject<>();
  }

  @Test(expected = NullPointerException.class)
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testRegisterNPE() {
    subject.register(null);
  }

  @Test(expected = NullPointerException.class)
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testUnregisterNPE() {
    subject.unregister(null);
  }

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testRegisterUnregister() {
    subject.register(OBSERVER_KEY);
    subject.register(OBSERVER_1_KEY);

    assertThat(1).isEqualTo(subject.fireApproveFromAll(testFunc, OBSERVER_1_KEY).size());

    subject.unregister(OBSERVER_1_KEY);
    assertThat(1).isEqualTo(subject.fireApproveFromAll(testFunc, OBSERVER_1_KEY).size());

    subject.unregister(OBSERVER_KEY);
    assertThat(0).isEqualTo(subject.fireApproveFromAll(testFunc, OBSERVER_1_KEY).size());
  }

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testFireApproveFromAllWithArg() {
    subject.register(OBSERVER_KEY);

    assertThat(0).isEqualTo(subject.fireApproveFromAll(testFunc, OBSERVER_KEY).size());
    assertThat(1).isEqualTo(subject.fireApproveFromAll(testFunc, OBSERVER_1_KEY).size());

    subject.register(OBSERVER_1_KEY);
    assertThat(1).isEqualTo(subject.fireApproveFromAll(testFunc, OBSERVER_1_KEY).size());
  }
}
