/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.authenticator;

import static io.harness.annotations.dev.HarnessTeam.DEL;
import static io.harness.data.encoding.EncodingUtils.decodeBase64ToString;
import static io.harness.eraro.ErrorCode.DEFAULT_ERROR_CODE;
import static io.harness.eraro.ErrorCode.EXPIRED_TOKEN;
import static io.harness.exception.WingsException.USER_ADMIN;
import static io.harness.manage.GlobalContextManager.initGlobalContextGuard;
import static io.harness.manage.GlobalContextManager.upsertGlobalContextRecord;

import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.context.GlobalContext;
import io.harness.delegate.beans.DelegateToken;
import io.harness.delegate.beans.DelegateToken.DelegateTokenKeys;
import io.harness.delegate.beans.DelegateTokenCacheKey;
import io.harness.delegate.beans.DelegateTokenStatus;
import io.harness.delegate.utils.DelegateTokenCacheHelper;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.InvalidTokenException;
import io.harness.exception.RevokedTokenException;
import io.harness.exception.WingsException;
import io.harness.globalcontex.DelegateTokenGlobalContextData;
import io.harness.manage.GlobalContextManager;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import io.harness.security.DelegateTokenAuthenticator;

import software.wings.beans.Account;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jwt.EncryptedJWT;
import java.text.ParseException;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.mongodb.morphia.query.Query;

@Slf4j
@Singleton
@OwnedBy(DEL)
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
public class DelegateTokenAuthenticatorImpl implements DelegateTokenAuthenticator {
  @Inject private DelegateTokenCacheHelper delegateTokenCacheHelper;
  @Inject private HPersistence persistence;

  private final LoadingCache<String, String> keyCache =
      Caffeine.newBuilder()
          .maximumSize(10000)
          .expireAfterWrite(5, TimeUnit.MINUTES)
          .build(accountId
              -> Optional.ofNullable(persistence.get(Account.class, accountId))
                     .map(Account::getAccountKey)
                     .orElse(null));

  @Override
  public void validateDelegateToken(String accountId, String tokenString) {
    EncryptedJWT encryptedJWT;
    try {
      encryptedJWT = EncryptedJWT.parse(tokenString);
    } catch (ParseException e) {
      throw new InvalidTokenException("Invalid delegate token format", USER_ADMIN);
    }
    String delegateHostName = "";
    try {
      delegateHostName = encryptedJWT.getJWTClaimsSet().getIssuer();
    } catch (Exception e) {
      log.warn("Couldn't parse delegate token");
    }
    DelegateTokenCacheKey delegateTokenCacheKey =
        DelegateTokenCacheKey.builder().accountId(accountId).delegateHostName(delegateHostName).build();
    boolean decryptedWithActiveToken = false;
    boolean decryptedWithRevokedToken = false;
    DelegateToken delegateTokenFromCache = delegateTokenCacheHelper.getDelegateToken(delegateTokenCacheKey);
    boolean decryptedWithTokenFromCache = decryptJwtTokenWithDelegateToken(encryptedJWT, delegateTokenFromCache);
    if (!decryptedWithTokenFromCache) {
      delegateTokenCacheHelper.invalidateCacheUsingKey(delegateTokenCacheKey);
      decryptedWithActiveToken =
          decryptJWTDelegateToken(delegateTokenCacheKey, DelegateTokenStatus.ACTIVE, encryptedJWT);
      if (!decryptedWithActiveToken) {
        decryptedWithRevokedToken =
            decryptJWTDelegateToken(delegateTokenCacheKey, DelegateTokenStatus.REVOKED, encryptedJWT);
      }
    }

    if ((decryptedWithTokenFromCache && delegateTokenFromCache.getStatus() == DelegateTokenStatus.REVOKED)
        || decryptedWithRevokedToken) {
      log.error("Delegate {} is using REVOKED delegate token {}", delegateHostName,
          delegateTokenFromCache == null ? "" : delegateTokenFromCache.getName());
      throw new RevokedTokenException("Invalid delegate token. Delegate is using revoked token", USER_ADMIN);
    }

    if (!decryptedWithTokenFromCache && !decryptedWithActiveToken && !decryptedWithRevokedToken) {
      // we are not able to decrypt with token, trying with accountKey now
      decryptWithAccountKey(accountId, encryptedJWT);
    }

    try {
      Date expirationDate = encryptedJWT.getJWTClaimsSet().getExpirationTime();
      if (System.currentTimeMillis() > expirationDate.getTime()) {
        throw new InvalidRequestException("Unauthorized", EXPIRED_TOKEN, null);
      }
    } catch (ParseException ex) {
      throw new InvalidRequestException("Unauthorized", ex, EXPIRED_TOKEN, null);
    }
  }

  private void decryptWithAccountKey(String accountId, EncryptedJWT encryptedJWT) {
    String accountKey = null;
    try {
      accountKey = keyCache.get(accountId);
    } catch (Exception ex) {
      log.warn("Account key not found for accountId: {}", accountId, ex);
    }

    if (accountKey == null || GLOBAL_ACCOUNT_ID.equals(accountId)) {
      throw new InvalidRequestException("Access denied", USER_ADMIN);
    }

    try {
      decryptDelegateToken(encryptedJWT, accountKey);
    } catch (InvalidTokenException e) {
      log.error("Delegate is using invalid account key.");
      throw new InvalidTokenException("Invalid delegate token", USER_ADMIN);
    }
  }

  // TODO: make sure that cg delegate is not using ng token and vice-versa.
  private boolean decryptJWTDelegateToken(
      DelegateTokenCacheKey delegateTokenCacheKey, DelegateTokenStatus status, EncryptedJWT encryptedJWT) {
    long startTime = System.currentTimeMillis();
    // first try to decrypt with cg tokens and if we failed then try with ng tokens
    Query<DelegateToken> query = persistence.createQuery(DelegateToken.class)
                                     .field(DelegateTokenKeys.accountId)
                                     .equal(delegateTokenCacheKey.getAccountId())
                                     .field(DelegateTokenKeys.status)
                                     .equal(status);

    boolean result = decryptDelegateTokenByQuery(query, delegateTokenCacheKey, encryptedJWT);
    long endTime = System.currentTimeMillis() - startTime;
    log.debug("Delegate Token verification for accountId {} and status {} took {} milliseconds.",
        delegateTokenCacheKey.getAccountId(), status.name(), endTime);
    return result;
  }

  // TODO: Arpit, check owner also and filter accordingly
  private boolean decryptDelegateTokenByQuery(
      Query query, DelegateTokenCacheKey delegateTokenCacheKey, EncryptedJWT encryptedJWT) {
    try (HIterator<DelegateToken> iterator = new HIterator<>(query.fetch())) {
      while (iterator.hasNext()) {
        DelegateToken delegateToken = iterator.next();
        if (decryptJwtTokenWithDelegateToken(encryptedJWT, delegateToken)) {
          delegateTokenCacheHelper.putToken(delegateTokenCacheKey, delegateToken);
          return true;
        }
      }
      return false;
    }
  }

  private boolean decryptJwtTokenWithDelegateToken(EncryptedJWT encryptedJWT, DelegateToken delegateToken) {
    if (delegateToken == null) {
      return false;
    }
    try {
      if (delegateToken.isNg()) {
        decryptDelegateToken(encryptedJWT, decodeBase64ToString(delegateToken.getValue()));
      } else {
        decryptDelegateToken(encryptedJWT, delegateToken.getValue());
      }
      if (delegateToken.getStatus() == DelegateTokenStatus.ACTIVE) {
        if (!GlobalContextManager.isAvailable()) {
          initGlobalContextGuard(new GlobalContext());
        }
        upsertGlobalContextRecord(DelegateTokenGlobalContextData.builder().tokenName(delegateToken.getName()).build());
      }
      return true;
    } catch (Exception e) {
      log.debug("Fail to decrypt Delegate JWT using delete token {} for the account {} from cache",
          delegateToken.getName(), delegateToken.getAccountId());
    }
    return false;
  }

  private void decryptDelegateToken(EncryptedJWT encryptedJWT, String delegateToken) {
    byte[] encodedKey;
    try {
      encodedKey = Hex.decodeHex(delegateToken.toCharArray());
    } catch (DecoderException e) {
      throw new WingsException(DEFAULT_ERROR_CODE, USER_ADMIN, e);
    }

    JWEDecrypter decrypter;
    try {
      decrypter = new DirectDecrypter(new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES"));
    } catch (KeyLengthException e) {
      throw new WingsException(DEFAULT_ERROR_CODE, USER_ADMIN, e);
    }

    try {
      encryptedJWT.decrypt(decrypter);
    } catch (JOSEException e) {
      throw new InvalidTokenException("Invalid delegate token", USER_ADMIN);
    }
  }
}
