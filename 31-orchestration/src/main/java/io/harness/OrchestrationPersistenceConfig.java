package io.harness;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.annotation.HarnessRepo;
import io.harness.beans.converters.SweepingOutputReadMongoConverter;
import io.harness.beans.converters.SweepingOutputWriteMongoConverter;
import io.harness.engine.executions.node.listeners.NodeExecutionAfterSaveListener;
import io.harness.engine.executions.plan.listeners.PlanExecutionAfterSaveListener;
import io.harness.exception.GeneralException;
import io.harness.mongo.OrchestrationTypeInformationMapper;
import io.harness.ng.SpringPersistenceConfig;
import io.harness.spring.AliasRegistrar;
import org.reflections.Reflections;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.convert.TypeInformationMapper;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.MongoTypeMapper;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@EnableMongoRepositories(basePackages = {"io.harness.engine"},
    includeFilters = @ComponentScan.Filter(HarnessRepo.class), mongoTemplateRef = "orchestrationMongoTemplate")
public class OrchestrationPersistenceConfig extends SpringPersistenceConfig {
  private Injector injector;

  @Inject
  public OrchestrationPersistenceConfig(Injector injector) {
    super(injector);
    this.injector = injector;
  }

  @Override
  public CustomConversions customConversions() {
    return new MongoCustomConversions(ImmutableList.of(injector.getInstance(SweepingOutputReadMongoConverter.class),
        injector.getInstance(SweepingOutputWriteMongoConverter.class)));
  }

  @Bean(name = "orchestrationMongoTemplate")
  @Primary
  public MongoTemplate orchestrationMongoTemplate() throws Exception {
    return new MongoTemplate(mongoDbFactory(), mappingMongoConverter());
  }

  // Node Execution Listener Beans
  @Bean
  public NodeExecutionAfterSaveListener nodeExecutionAfterSaveListener() {
    return new NodeExecutionAfterSaveListener();
  }

  // Plan Execution Listener Beans
  @Bean
  public PlanExecutionAfterSaveListener planExecutionAfterSaveListener() {
    PlanExecutionAfterSaveListener listener = new PlanExecutionAfterSaveListener();
    injector.injectMembers(listener);
    return listener;
  }

  @Bean
  @Override
  public MappingMongoConverter mappingMongoConverter() throws Exception {
    DbRefResolver dbRefResolver = new DefaultDbRefResolver(mongoDbFactory());
    TypeInformationMapper informationMapper =
        OrchestrationTypeInformationMapper.builder().aliasMap(collectAliasMap()).build();
    MongoTypeMapper typeMapper = new DefaultMongoTypeMapper(
        DefaultMongoTypeMapper.DEFAULT_TYPE_KEY, Collections.singletonList(informationMapper));
    MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, mongoMappingContext());
    converter.setCustomConversions(customConversions());
    converter.setCodecRegistryProvider(mongoDbFactory());
    converter.setTypeMapper(typeMapper);
    return converter;
  }

  private Map<String, Class<?>> collectAliasMap() {
    Map<String, Class<?>> aliases = new ConcurrentHashMap<>();
    try {
      Reflections reflections = new Reflections("io.harness.serializer.spring");
      for (Class clazz : reflections.getSubTypesOf(AliasRegistrar.class)) {
        Constructor<?> constructor = clazz.getConstructor();
        final AliasRegistrar aliasRegistrar = (AliasRegistrar) constructor.newInstance();
        aliasRegistrar.register(aliases);
      }
    } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
      throw new GeneralException("Failed initializing Spring Data Converters", e);
    }
    return aliases;
  }
}