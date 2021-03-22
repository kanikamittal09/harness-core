package software.wings.service.impl.yaml.handler.InfraDefinition;

import static io.harness.validation.Validator.notNullCheck;

import static java.lang.String.format;

import software.wings.beans.InfrastructureType;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.infra.PhysicalInfraWinrm;
import software.wings.infra.PhysicalInfraWinrmYaml;
import software.wings.service.impl.yaml.handler.CloudProviderInfrastructure.CloudProviderInfrastructureYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.SettingsService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;

@Singleton
public class PhysicalInfraWinrmYamlHandler
    extends CloudProviderInfrastructureYamlHandler<PhysicalInfraWinrmYaml, PhysicalInfraWinrm> {
  @Inject private YamlHelper yamlHelper;
  @Inject private SettingsService settingsService;
  @Override
  public PhysicalInfraWinrmYaml toYaml(PhysicalInfraWinrm bean, String appId) {
    SettingAttribute cloudProvider = settingsService.get(bean.getCloudProviderId());
    SettingAttribute winRmConnectionAttr = settingsService.get(bean.getWinRmConnectionAttributes());
    return PhysicalInfraWinrmYaml.builder()
        .hosts(bean.getHosts())
        .hostNames(bean.getHostNames())
        .winRmConnectionAttributesName(winRmConnectionAttr.getName())
        .loadBalancerName(bean.getLoadBalancerId())
        .cloudProviderName(cloudProvider.getName())
        .type(InfrastructureType.PHYSICAL_INFRA_WINRM)
        .build();
  }

  @Override
  public PhysicalInfraWinrm upsertFromYaml(
      ChangeContext<PhysicalInfraWinrmYaml> changeContext, List<ChangeContext> changeSetContext) {
    PhysicalInfraWinrm physicalInfraWinrm = PhysicalInfraWinrm.builder().build();
    toBean(physicalInfraWinrm, changeContext);
    return physicalInfraWinrm;
  }

  private void toBean(PhysicalInfraWinrm bean, ChangeContext<PhysicalInfraWinrmYaml> changeContext) {
    PhysicalInfraWinrmYaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    SettingAttribute cloudProvider = settingsService.getSettingAttributeByName(accountId, yaml.getCloudProviderName());
    SettingAttribute winRmConnectionAttr =
        settingsService.getSettingAttributeByName(accountId, yaml.getWinRmConnectionAttributesName());
    notNullCheck(format("Cloud Provider with name %s does not exist", yaml.getCloudProviderName()), cloudProvider);
    notNullCheck(format("winRmConnectionAttr with name %s does not exist", yaml.getWinRmConnectionAttributesName()),
        winRmConnectionAttr);
    bean.setCloudProviderId(cloudProvider.getUuid());
    bean.setHosts(yaml.getHosts());
    bean.setHostNames(yaml.getHostNames());
    bean.setWinRmConnectionAttributes(winRmConnectionAttr.getUuid());
    bean.setLoadBalancerName(yaml.getLoadBalancerName());
    bean.setLoadBalancerId(yaml.getLoadBalancerName());
  }

  @Override
  public Class getYamlClass() {
    return PhysicalInfraWinrmYaml.class;
  }
}
