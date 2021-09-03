package io.harness.feature.services.impl;

import static io.harness.rule.OwnerRule.ZHUO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.ModuleType;
import io.harness.category.element.UnitTests;
import io.harness.feature.bases.EnableDisableRestriction;
import io.harness.feature.bases.Feature;
import io.harness.feature.bases.Restriction;
import io.harness.feature.bases.StaticLimitRestriction;
import io.harness.feature.beans.FeatureDetailsDTO;
import io.harness.feature.beans.RestrictionDTO;
import io.harness.feature.cache.LicenseInfoCache;
import io.harness.feature.configs.FeatureName;
import io.harness.feature.constants.RestrictionType;
import io.harness.feature.exceptions.LimitExceededException;
import io.harness.feature.handlers.RestrictionHandlerFactory;
import io.harness.feature.interfaces.StaticLimitInterface;
import io.harness.licensing.Edition;
import io.harness.licensing.beans.summary.CDLicenseSummaryDTO;
import io.harness.licensing.beans.summary.CILicenseSummaryDTO;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class FeatureServiceImplTest extends CategoryTest {
  FeatureServiceImpl featureService;
  private LicenseInfoCache licenseInfoCache;
  private RestrictionHandlerFactory restrictionHandlerFactory;
  Feature feature;
  Feature featureNotEnabled;
  Feature ciFeature;
  private static final FeatureName FEATURE_NAME = FeatureName.TEST1;
  private static final FeatureName FEATURE_NAME_STATIC = FeatureName.TEST2;
  private static final FeatureName CI_FEATURE_NAME = FeatureName.TEST3;
  private static final String ACCOUNT_ID = "1";

  @Before
  public void setup() {
    restrictionHandlerFactory = new RestrictionHandlerFactory();
    licenseInfoCache = mock(LicenseInfoCache.class);
    featureService = new FeatureServiceImpl(licenseInfoCache, restrictionHandlerFactory);
    feature = new Feature(FEATURE_NAME, "description", ModuleType.CD,
        ImmutableMap.<Edition, Restriction>builder()
            .put(Edition.FREE, new EnableDisableRestriction(RestrictionType.AVAILABILITY, true))
            .build());
    featureNotEnabled = new Feature(FEATURE_NAME_STATIC, "description", ModuleType.CD,
        ImmutableMap.<Edition, Restriction>builder()
            .put(Edition.FREE,
                new StaticLimitRestriction(RestrictionType.STATIC_LIMIT, 2,
                    new StaticLimitInterface() {
                      @Override
                      public long getCurrentValue(String accountIdentifier) {
                        return 3;
                      }
                    }))
            .put(Edition.TEAM, new EnableDisableRestriction(RestrictionType.AVAILABILITY, false))
            .put(Edition.ENTERPRISE, new EnableDisableRestriction(RestrictionType.AVAILABILITY, true))
            .build());
    ciFeature = new Feature(CI_FEATURE_NAME, "description", ModuleType.CI,
        ImmutableMap.<Edition, Restriction>builder()
            .put(Edition.FREE, new EnableDisableRestriction(RestrictionType.AVAILABILITY, false))
            .build());
    featureService.registerFeature(FEATURE_NAME, feature);
    featureService.registerFeature(FEATURE_NAME_STATIC, featureNotEnabled);
    featureService.registerFeature(CI_FEATURE_NAME, ciFeature);

    when(licenseInfoCache.getLicenseInfo(any(), eq(ModuleType.CD)))
        .thenReturn(CDLicenseSummaryDTO.builder()
                        .maxExpiryTime(Long.MAX_VALUE)
                        .edition(Edition.FREE)
                        .moduleType(ModuleType.CD)
                        .build());
    when(licenseInfoCache.getLicenseInfo(any(), eq(ModuleType.CI)))
        .thenReturn(CILicenseSummaryDTO.builder()
                        .maxExpiryTime(Long.MAX_VALUE)
                        .edition(Edition.FREE)
                        .moduleType(ModuleType.CI)
                        .build());
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testIsFeatureAvailable() {
    boolean result = featureService.isFeatureAvailable(FEATURE_NAME, ACCOUNT_ID);
    assertThat(result).isTrue();
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testCheckAvailabilityOrThrowSucceed() {
    featureService.checkAvailabilityOrThrow(FEATURE_NAME, ACCOUNT_ID);
  }

  @Test(expected = LimitExceededException.class)
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testCheckAvailabilityOrThrowFailed() {
    featureService.checkAvailabilityOrThrow(FEATURE_NAME_STATIC, ACCOUNT_ID);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testGetFeatureDetails() {
    FeatureDetailsDTO dto =
        FeatureDetailsDTO.builder()
            .name(FEATURE_NAME.name())
            .description("description")
            .moduleType(ModuleType.CD.name())
            .restriction(RestrictionDTO.builder().enabled(true).restrictionType(RestrictionType.AVAILABILITY).build())
            .build();
    FeatureDetailsDTO featureDetail = featureService.getFeatureDetail(FEATURE_NAME, ACCOUNT_ID);
    assertThat(featureDetail).isEqualTo(dto);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testGetEnabledFeatures() {
    List<FeatureDetailsDTO> result = featureService.getEnabledFeatureDetails(ACCOUNT_ID, ModuleType.CD);
    assertThat(result.size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testGetAllFeatureNames() {
    Set<FeatureName> result = featureService.getAllFeatureNames();
    assertThat(result.size()).isEqualTo(FeatureName.values().length);
  }
}
