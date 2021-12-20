package common.oauth2;

import com.google.common.collect.ImmutableList;
import it.besmartbeopen.keycloak.api.UsersApi;
import it.besmartbeopen.keycloak.model.*;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class UsersApiFallback implements UsersApi {

  private final Exception exception;

  @Override
  public Integer realmUsersCountGet(String s, String s1, String s2, String s3, String s4, String s5) {
    return 0;
  }

  @Override
  public Integer realmUsersCountGet(String s, Map<String, Object> map) {
    log.error("get users count error: {}", exception.getMessage());
    return 0;
  }

  @Override
  public List<UserRepresentation> realmUsersGet(String s, Boolean aBoolean, String s1, Integer integer, String s2, String s3, Integer integer1, String s4, String s5) {
    return ImmutableList.of();
  }

  @Override
  public List<UserRepresentation> realmUsersGet(String s, Map<String, Object> map) {
    return ImmutableList.of();
  }

  @Override
  public List<String> realmUsersIdConfiguredUserStorageCredentialTypesGet(String s, String s1) {
    return ImmutableList.of();
  }

  @Override
  public void realmUsersIdConsentsClientDelete(String s, String s1, String s2) {
  }

  @Override
  public List<Map<String, Object>> realmUsersIdConsentsGet(String s, String s1) {
    return ImmutableList.of();
  }

  @Override
  public void realmUsersIdCredentialsCredentialIdDelete(String s, String s1, String s2) {

  }

  @Override
  public void realmUsersIdCredentialsCredentialIdMoveAfterNewPreviousCredentialIdPost(String s, String s1, String s2, String s3) {

  }

  @Override
  public void realmUsersIdCredentialsCredentialIdMoveToFirstPost(String s, String s1, String s2) {

  }

  @Override
  public void realmUsersIdCredentialsCredentialIdUserLabelPut(String s, String s1, String s2, String s3) {

  }

  @Override
  public List<CredentialRepresentation> realmUsersIdCredentialsGet(String s, String s1) {
    return ImmutableList.of();
  }

  @Override
  public void realmUsersIdDelete(String s, String s1) {

  }

  @Override
  public void realmUsersIdDisableCredentialTypesPut(String s, String s1, List<String> list) {

  }

  @Override
  public void realmUsersIdExecuteActionsEmailPut(String s, String s1, List<String> list, String s2, Integer integer, String s3) {

  }

  @Override
  public void realmUsersIdExecuteActionsEmailPut(String s, String s1, List<String> list, Map<String, Object> map) {

  }

  @Override
  public List<FederatedIdentityRepresentation> realmUsersIdFederatedIdentityGet(String s, String s1) {
    return ImmutableList.of();
  }

  @Override
  public void realmUsersIdFederatedIdentityProviderDelete(String s, String s1, String s2) {

  }

  @Override
  public void realmUsersIdFederatedIdentityProviderPost(String s, String s1, String s2, FederatedIdentityRepresentation federatedIdentityRepresentation) {

  }

  @Override
  public UserRepresentation realmUsersIdGet(String s, String s1) {
    log.error("get user by id \"{}\" from keycloak: {}", s1, exception.getMessage());
    return null;
  }

  @Override
  public Map<String, Object> realmUsersIdGroupsCountGet(String s, String s1, String s2) {
    return null;
  }

  @Override
  public Map<String, Object> realmUsersIdGroupsCountGet(String s, String s1, Map<String, Object> map) {
    return null;
  }

  @Override
  public List<GroupRepresentation> realmUsersIdGroupsGet(String s, String s1, Boolean aBoolean, Integer integer, Integer integer1, String s2) {
    return null;
  }

  @Override
  public List<GroupRepresentation> realmUsersIdGroupsGet(String s, String s1, Map<String, Object> map) {
    return null;
  }

  @Override
  public void realmUsersIdGroupsGroupIdDelete(String s, String s1, String s2) {

  }

  @Override
  public void realmUsersIdGroupsGroupIdPut(String s, String s1, String s2) {

  }

  @Override
  public Map<String, Object> realmUsersIdImpersonationPost(String s, String s1) {
    return null;
  }

  @Override
  public void realmUsersIdLogoutPost(String s, String s1) {

  }

  @Override
  public List<UserSessionRepresentation> realmUsersIdOfflineSessionsClientIdGet(String s, String s1, String s2) {
    return null;
  }

  @Override
  public void realmUsersIdPut(String s, String s1, UserRepresentation userRepresentation) {

  }

  @Override
  public void realmUsersIdResetPasswordPut(String s, String s1, CredentialRepresentation credentialRepresentation) {

  }

  @Override
  public void realmUsersIdSendVerifyEmailPut(String s, String s1, String s2, String s3) {

  }

  @Override
  public void realmUsersIdSendVerifyEmailPut(String s, String s1, Map<String, Object> map) {

  }

  @Override
  public List<UserSessionRepresentation> realmUsersIdSessionsGet(String s, String s1) {
    return null;
  }

  @Override
  public void realmUsersPost(String s, UserRepresentation userRepresentation) {
    log.warn("user creation on keycloak: {}, {}", userRepresentation, exception.getMessage());
  }
}
