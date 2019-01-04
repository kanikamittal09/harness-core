package software.wings.service.intfc.yaml;

import software.wings.beans.Event.Type;
import software.wings.beans.yaml.Change.ChangeType;

public interface YamlPushService {
  <T> void pushYamlChangeSet(
      String accountId, T oldEntity, T newEntity, Type type, boolean syncFromGit, boolean isRename);

  <R, T> void pushYamlChangeSet(String accountId, R helperEntity, T entity, Type type, boolean syncFromGit);

  void pushYamlChangeSet(String accountId, String appId, ChangeType changeType, boolean syncFromGit);
}
