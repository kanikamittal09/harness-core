package software.wings.security.authentication.oauth;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import software.wings.beans.User;
import software.wings.beans.sso.OauthSettings;
import software.wings.security.authentication.AuthenticationUtils;
import software.wings.security.authentication.OauthProviderType;
import software.wings.security.saml.SSORequest;
import software.wings.service.impl.SSOSettingServiceImpl;

import java.util.List;

public class OauthOptions {
  @Inject GithubClientImpl githubClient;
  @Inject LinkedinClientImpl linkedinClient;
  @Inject GoogleClientImpl googleClient;
  @Inject AzureClientImpl azureClient;
  @Inject AuthenticationUtils authenticationUtils;
  @Inject SSOSettingServiceImpl ssoSettingService;
  @Inject BitbucketClient bitbucketClient;
  @Inject GitlabClient gitlabClient;

  public String getRedirectURI(OauthProviderType oauthProvider) {
    return getOauthProvider(oauthProvider).getRedirectUrl().toString();
  }

  public OauthClient getOauthProvider(OauthProviderType oauthProvider) {
    switch (oauthProvider) {
      case GITHUB:
        return githubClient;
      case LINKEDIN:
        return linkedinClient;
      case GOOGLE:
        return googleClient;
      case AZURE:
        return azureClient;
      case BITBUCKET:
        return bitbucketClient;
      case GITLAB:
        return gitlabClient;
      default:
        throw new InvalidRequestException(String.format("Oauth provider %s not supported.", oauthProvider));
    }
  }

  public SSORequest oauthProviderRedirectionUrl(User user) {
    OauthSettings oauthSettings = ssoSettingService.getOauthSettingsByAccountId(user.getDefaultAccountId());

    if (null == oauthSettings || isEmpty(oauthSettings.getAllowedProviders())) {
      throw new WingsException("Could not fetch OAuth settings");
    }

    List<OauthProviderType> oauthProviderTypes = Lists.newArrayList(oauthSettings.getAllowedProviders());
    OauthProviderType defaultOAuthProviderType = oauthProviderTypes.get(0);

    return new SSORequest(defaultOAuthProviderType, getRedirectURI(defaultOAuthProviderType), oauthProviderTypes);
  }
}
