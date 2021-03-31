package io.harness.connector.accesscontrol;

import static io.harness.annotations.dev.HarnessTeam.DX;
import io.harness.annotations.dev.OwnedBy;
import lombok.experimental.UtilityClass;

@OwnedBy(DX)
@UtilityClass
public class ConnectorsAccessControlPermissions {
  public static final String CREATE_CONNECTOR_PERMISSION = "core_connector_create";
  public static final String VIEW_CONNECTOR_PERMISSION = "core_connector_view";
  public static final String EDIT_CONNECTOR_PERMISSION = "core_connector_edit";
  public static final String DELETE_CONNECTOR_PERMISSION = "core.connector.delete";
}
