package software.wings.service.impl;

import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;

import com.google.common.collect.ImmutableMap;

import org.eclipse.jetty.util.ArrayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ConfigFile;
import software.wings.beans.Host;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.Tag;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.TagService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by anubhaw on 4/4/16.
 */
@Singleton
public class ServiceTemplateServiceImpl implements ServiceTemplateService {
  private WingsPersistence wingsPersistence;
  private TagService tagService;
  private ConfigService configService;
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject
  public ServiceTemplateServiceImpl(
      WingsPersistence wingsPersistence, TagService tagService, ConfigService configService) {
    this.wingsPersistence = wingsPersistence;
    this.tagService = tagService;
    this.configService = configService;
  }

  @Override
  public PageResponse<ServiceTemplate> list(PageRequest<ServiceTemplate> pageRequest) {
    return wingsPersistence.query(ServiceTemplate.class, pageRequest);
  }

  @Override
  public ServiceTemplate save(ServiceTemplate serviceTemplate) {
    return wingsPersistence.saveAndGet(ServiceTemplate.class, serviceTemplate);
  }

  @Override
  public ServiceTemplate update(ServiceTemplate serviceTemplate) {
    wingsPersistence.updateFields(ServiceTemplate.class, serviceTemplate.getUuid(),
        ImmutableMap.of("name", serviceTemplate.getName(), "description", serviceTemplate.getDescription(), "service",
            serviceTemplate.getService()));
    return wingsPersistence.get(ServiceTemplate.class, serviceTemplate.getUuid());
  }

  @Override
  public ServiceTemplate updateHostAndTags(
      String appId, String envId, String serviceTemplateId, List<String> tagIds, List<String> hostIds) {
    List<Tag> tags = new ArrayList<>();
    if (tagIds != null) {
      for (String tagId : tagIds) {
        tags.add(wingsPersistence.get(Tag.class, tagId));
      }
    }
    List<Host> hosts = new ArrayList<>();
    if (hostIds != null) {
      for (String hostId : hostIds) {
        hosts.add(wingsPersistence.get(Host.class, hostId));
      }
    }
    wingsPersistence.updateFields(
        ServiceTemplate.class, serviceTemplateId, ImmutableMap.of("hosts", hosts, "tags", tags));
    return wingsPersistence.get(ServiceTemplate.class, serviceTemplateId);
  }

  @Override
  public Map<String, List<ConfigFile>> computedConfigFiles(String appId, String envId, String templateId) {
    ServiceTemplate serviceTemplate = wingsPersistence.get(ServiceTemplate.class, templateId);
    if (serviceTemplate == null) {
      return new HashMap<>();
    }
    /* override order(left to right): Service -> Env -> [Tag Hierarchy] -> Host */

    List<ConfigFile> serviceConfigFiles =
        configService.getConfigFilesForEntity(DEFAULT_TEMPLATE_ID, serviceTemplate.getService().getUuid());
    List<ConfigFile> envConfigFiles = configService.getConfigFilesForEntity(templateId, serviceTemplate.getEnvId());

    // service -> env overrides
    logger.info("Apply env config.");
    List<ConfigFile> envComputedConfigFiles = overrideConfigFiles(serviceConfigFiles, envConfigFiles);

    // flatten tag hierarchy and [tag -> tag] overrides
    logger.info("Flatten Tag hierarchy and apply config overrides");
    List<Tag> leafTagNodes = applyOverrideAndGetLeafTags(serviceTemplate);

    // env->tag override
    logger.info("Apply tag override on tags");
    for (Tag tag : leafTagNodes) {
      tag.setConfigFiles(overrideConfigFiles(envComputedConfigFiles, tag.getConfigFiles()));
    }

    // Host override
    logger.info("Apply host overrides");
    Map<String, List<ConfigFile>> computedHostConfigs = new HashMap<>();

    // Untagged hosts override: env->host
    logger.info("Apply host overrides for untagged hosts");
    for (Host host : serviceTemplate.getHosts()) {
      List<ConfigFile> configFiles = configService.getConfigFilesForEntity(templateId, host.getUuid());
      computedHostConfigs.put(host.getUuid(), overrideConfigFiles(envConfigFiles, configFiles));
    }

    // Tagged hosts
    logger.info("Apply host overrides for tagged hosts");
    for (Tag tag : leafTagNodes) {
      List<Host> taggedHosts = wingsPersistence.createQuery(Host.class).field("tags").equal(tag.getUuid()).asList();
      for (Host host : taggedHosts) {
        computedHostConfigs.put(host.getUuid(),
            overrideConfigFiles(
                tag.getConfigFiles(), configService.getConfigFilesForEntity(templateId, host.getUuid())));
      }
    }
    return computedHostConfigs;
  }

  @Override
  public void delete(String appId, String envId, String serviceTemplateId) {
    wingsPersistence.delete(ServiceTemplate.class, serviceTemplateId);
  }

  @Override
  public List<ConfigFile> overrideConfigFiles(List<ConfigFile> existingFiles, List<ConfigFile> newFiles) {
    logger.info("Config files before overrides [{}]", existingFiles.toString());
    logger.info("New override config files [{}]", newFiles != null ? newFiles.toString() : null);
    if (newFiles != null && !newFiles.isEmpty()) {
      existingFiles = Stream.concat(newFiles.stream(), existingFiles.stream())
                          .filter(new TreeSet<>(Comparator.comparing(ConfigFile::getName))::add)
                          .collect(Collectors.toList());
    }
    logger.info("Config files after overrides [{}]", existingFiles.toString());
    return existingFiles;
  }

  private List<Tag> applyOverrideAndGetLeafTags(ServiceTemplate serviceTemplate) {
    List<Tag> leafTagNodes = new ArrayList<>();
    List<Tag> rootTags = tagService.getRootConfigTags(serviceTemplate.getEnvId());
    for (Tag tag : rootTags) {
      tag.getConfigFiles().addAll(configService.getConfigFilesForEntity(serviceTemplate.getUuid(), tag.getUuid()));
    }

    Queue<Tag> queue = new ArrayQueue<>();
    queue.addAll(rootTags);

    while (!queue.isEmpty()) {
      Tag root = queue.poll();
      leafTagNodes.add(root);
      for (Tag child : root.getLinkedTags()) {
        child.getConfigFiles().addAll(
            configService.getConfigFilesForEntity(serviceTemplate.getUuid(), child.getUuid()));
        child.setConfigFiles(overrideConfigFiles(root.getConfigFiles(), child.getConfigFiles()));
        queue.add(child);
      }
    }
    return leafTagNodes;
  }
}
