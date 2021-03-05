package software.wings.service.impl.yaml.handler.InfraDefinition;

import static io.harness.validation.Validator.notNullCheck;

import static java.lang.String.format;

import software.wings.beans.InfrastructureType;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.infra.CodeDeployInfrastructure;
import software.wings.infra.CodeDeployInfrastructureYaml;
import software.wings.service.impl.yaml.handler.CloudProviderInfrastructure.CloudProviderInfrastructureYamlHandler;
import software.wings.service.intfc.SettingsService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;

@Singleton
public class CodeDeployInfrastructureYamlHandler
    extends CloudProviderInfrastructureYamlHandler<CodeDeployInfrastructureYaml, CodeDeployInfrastructure> {
  @Inject private SettingsService settingsService;
  @Override
  public CodeDeployInfrastructureYaml toYaml(CodeDeployInfrastructure bean, String appId) {
    SettingAttribute cloudProvider = settingsService.get(bean.getCloudProviderId());
    return CodeDeployInfrastructureYaml.builder()
        .region(bean.getRegion())
        .applicationName(bean.getApplicationName())
        .deploymentConfig(bean.getDeploymentConfig())
        .deploymentGroup(bean.getDeploymentGroup())
        .hostNameConvention(bean.getHostNameConvention())
        .cloudProviderName(cloudProvider.getName())
        .type(InfrastructureType.CODE_DEPLOY)
        .build();
  }

  @Override
  public CodeDeployInfrastructure upsertFromYaml(
      ChangeContext<CodeDeployInfrastructureYaml> changeContext, List<ChangeContext> changeSetContext) {
    CodeDeployInfrastructure bean = CodeDeployInfrastructure.builder().build();
    toBean(bean, changeContext);
    return bean;
  }

  private void toBean(CodeDeployInfrastructure bean, ChangeContext<CodeDeployInfrastructureYaml> changeContext) {
    CodeDeployInfrastructureYaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    SettingAttribute cloudProvider = settingsService.getSettingAttributeByName(accountId, yaml.getCloudProviderName());
    notNullCheck(format("Cloud Provider with name %s does not exist", yaml.getCloudProviderName()), cloudProvider);
    bean.setCloudProviderId(cloudProvider.getUuid());
    bean.setApplicationName(yaml.getApplicationName());
    bean.setDeploymentConfig(yaml.getDeploymentConfig());
    bean.setDeploymentGroup(yaml.getDeploymentGroup());
    bean.setHostNameConvention(yaml.getHostNameConvention());
    bean.setRegion(yaml.getRegion());
  }

  @Override
  public Class getYamlClass() {
    return CodeDeployInfrastructureYaml.class;
  }
}
