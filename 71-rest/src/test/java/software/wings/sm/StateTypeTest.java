package software.wings.sm;

import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_UNDERSCORE;
import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * Created by peeyushaggarwal on 6/6/16.
 */
@RunWith(JUnitParamsRunner.class)
public class StateTypeTest extends CategoryTest {
  private Object[][] getData() {
    Object[][] data = new Object[StateType.values().length][1];

    for (int i = 0; i < StateType.values().length; i++) {
      data[i][0] = UPPER_UNDERSCORE.to(UPPER_CAMEL, StateType.values()[i].name());
    }
    return data;
  }

  /**
   * Should create new instance for.
   *
   * @param stateTypeName the state type name
   * @throws Exception the exception
   */
  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  @Parameters(method = "getData")
  public void shouldCreateNewInstanceFor(String stateTypeName) throws Exception {
    StateType stateType = StateType.valueOf(UPPER_CAMEL.to(UPPER_UNDERSCORE, stateTypeName));
    assertThat(stateType).isNotNull();
    assertThat(stateType.newInstance("name")).isNotNull().extracting(State::getName).isEqualTo("name");
  }
}
