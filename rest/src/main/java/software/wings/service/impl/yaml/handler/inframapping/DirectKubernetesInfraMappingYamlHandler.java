package software.wings.service.impl.yaml.handler.inframapping;

import static software.wings.utils.Util.isEmpty;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.DirectKubernetesInfrastructureMapping.Yaml;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.intfc.security.SecretManager;
import software.wings.utils.Validator;

import java.util.List;

/**
 * @author rktummala on 10/22/17
 */
public class DirectKubernetesInfraMappingYamlHandler
    extends InfraMappingYamlHandler<Yaml, DirectKubernetesInfrastructureMapping> {
  private static final Logger logger = LoggerFactory.getLogger(DirectKubernetesInfraMappingYamlHandler.class);
  @Inject SecretManager secretManager;

  @Override
  public Yaml toYaml(DirectKubernetesInfrastructureMapping bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setType(InfrastructureMappingType.DIRECT_KUBERNETES.name());
    yaml.setMasterUrl(bean.getMasterUrl());
    yaml.setUsername(bean.getUsername());
    yaml.setNamespace(bean.getNamespace());

    String fieldName = null;
    String encryptedYamlRef;
    try {
      if (bean.getEncryptedPassword() != null) {
        fieldName = "password";
        encryptedYamlRef = secretManager.getEncryptedYamlRef(bean, fieldName);
        yaml.setPassword(encryptedYamlRef);
      }

      if (bean.getEncryptedCaCert() != null) {
        fieldName = "caCert";
        encryptedYamlRef = secretManager.getEncryptedYamlRef(bean, fieldName);
        yaml.setCaCert(encryptedYamlRef);
      }

      if (bean.getEncryptedClientCert() != null) {
        fieldName = "clientCert";
        encryptedYamlRef = secretManager.getEncryptedYamlRef(bean, fieldName);
        yaml.setClientCert(encryptedYamlRef);
      }

      if (bean.getEncryptedClientKey() != null) {
        fieldName = "clientKey";
        encryptedYamlRef = secretManager.getEncryptedYamlRef(bean, fieldName);
        yaml.setClientKey(encryptedYamlRef);
      }

    } catch (IllegalAccessException e) {
      logger.warn("Invalid " + fieldName + ". Should be a valid url to a secret");
      throw new WingsException(e);
    }
    return yaml;
  }

  @Override
  public DirectKubernetesInfrastructureMapping upsertFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    ensureValidChange(changeContext, changeSetContext);

    Yaml infraMappingYaml = changeContext.getYaml();
    String yamlFilePath = changeContext.getChange().getFilePath();
    String appId = yamlHelper.getAppId(changeContext.getChange().getAccountId(), yamlFilePath);
    Validator.notNullCheck("Couldn't retrieve app from yaml:" + yamlFilePath, appId);
    String envId = yamlHelper.getEnvironmentId(appId, yamlFilePath);
    Validator.notNullCheck("Couldn't retrieve environment from yaml:" + yamlFilePath, envId);
    String serviceId = getServiceId(appId, infraMappingYaml.getServiceName());
    Validator.notNullCheck("Couldn't retrieve service from yaml:" + yamlFilePath, serviceId);

    DirectKubernetesInfrastructureMapping current = new DirectKubernetesInfrastructureMapping();
    toBean(current, changeContext, appId, envId, serviceId);

    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());
    DirectKubernetesInfrastructureMapping previous =
        (DirectKubernetesInfrastructureMapping) infraMappingService.getInfraMappingByName(appId, envId, name);

    if (previous != null) {
      current.setUuid(previous.getUuid());
      return (DirectKubernetesInfrastructureMapping) infraMappingService.update(current);
    } else {
      return (DirectKubernetesInfrastructureMapping) infraMappingService.save(current);
    }
  }

  private void toBean(DirectKubernetesInfrastructureMapping bean, ChangeContext<Yaml> changeContext, String appId,
      String envId, String serviceId) throws HarnessException {
    Yaml infraMappingYaml = changeContext.getYaml();

    super.toBean(changeContext, bean, appId, envId, serviceId);
    bean.setMasterUrl(infraMappingYaml.getMasterUrl());
    bean.setUsername(infraMappingYaml.getUsername());
    bean.setNamespace(infraMappingYaml.getNamespace());

    // We need to set these fields for save / update to go through.
    bean.setComputeProviderSettingId("DIRECT");
    bean.setComputeProviderType("DIRECT");

    bean.setEncryptedPassword(infraMappingYaml.getPassword());
    bean.setEncryptedCaCert(infraMappingYaml.getCaCert());
    bean.setEncryptedClientCert(infraMappingYaml.getClientCert());
    bean.setEncryptedClientKey(infraMappingYaml.getClientKey());

    // TODO, UI generates this field internally. Its not exposed as a field. Need to find out why.
    bean.setClusterName("clusterName");
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    Yaml infraMappingYaml = changeContext.getYaml();
    return !(isEmpty(infraMappingYaml.getDeploymentType()) || isEmpty(infraMappingYaml.getInfraMappingType())
        || isEmpty(infraMappingYaml.getServiceName()) || isEmpty(infraMappingYaml.getType())
        || isEmpty(infraMappingYaml.getMasterUrl()));
  }

  @Override
  public DirectKubernetesInfrastructureMapping get(String accountId, String yamlFilePath) {
    return (DirectKubernetesInfrastructureMapping) yamlHelper.getInfraMapping(accountId, yamlFilePath);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
