package common.oauth2;

import it.besmartbeopen.keycloak.api.RealmsAdminApi;
import it.besmartbeopen.keycloak.model.*;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class RealmsAdminApiFallback implements RealmsAdminApi {
  private final Exception exception;

  @Override
  public void realmAdminEventsDelete(String s) {
  }

  @Override
  public List<AdminEventRepresentation> realmAdminEventsGet(String s, String s1, String s2, String s3, String s4, String s5, String s6, Integer integer, Integer integer1, List<String> list, String s7, List<String> list1) {
    return null;
  }

  @Override
  public List<AdminEventRepresentation> realmAdminEventsGet(String s, Map<String, Object> map) {
    return null;
  }

  @Override
  public void realmClearKeysCachePost(String s) {

  }

  @Override
  public void realmClearRealmCachePost(String s) {

  }

  @Override
  public void realmClearUserCachePost(String s) {

  }

  @Override
  public ClientRepresentation realmClientDescriptionConverterPost(String s, String s1) {
    return null;
  }

  @Override
  public List<Map<String, Object>> realmClientSessionStatsGet(String s) {
    return null;
  }

  @Override
  public List<String> realmCredentialRegistratorsGet(String s) {
    return null;
  }

  @Override
  public void realmDefaultDefaultClientScopesClientScopeIdDelete(String s, String s1) {

  }

  @Override
  public void realmDefaultDefaultClientScopesClientScopeIdPut(String s, String s1) {

  }

  @Override
  public List<ClientScopeRepresentation> realmDefaultDefaultClientScopesGet(String s) {
    return null;
  }

  @Override
  public List<GroupRepresentation> realmDefaultGroupsGet(String s) {
    return null;
  }

  @Override
  public void realmDefaultGroupsGroupIdDelete(String s, String s1) {

  }

  @Override
  public void realmDefaultGroupsGroupIdPut(String s, String s1) {

  }

  @Override
  public void realmDefaultOptionalClientScopesClientScopeIdDelete(String s, String s1) {

  }

  @Override
  public void realmDefaultOptionalClientScopesClientScopeIdPut(String s, String s1) {

  }

  @Override
  public List<ClientScopeRepresentation> realmDefaultOptionalClientScopesGet(String s) {
    return null;
  }

  @Override
  public void realmDelete(String s) {

  }

  @Override
  public RealmEventsConfigRepresentation realmEventsConfigGet(String s) {
    return null;
  }

  @Override
  public void realmEventsConfigPut(String s, RealmEventsConfigRepresentation realmEventsConfigRepresentation) {

  }

  @Override
  public void realmEventsDelete(String s) {

  }

  @Override
  public List<EventRepresentation> realmEventsGet(String s, String s1, String s2, String s3, Integer integer, String s4, Integer integer1, List<String> list, String s5) {
    return null;
  }

  @Override
  public List<EventRepresentation> realmEventsGet(String s, Map<String, Object> map) {
    log.error("realm-admin-api.realmEventsGet({}, {})", s, map, exception);
    return List.of();
  }

  @Override
  public RealmRepresentation realmGet(String s) {
    return null;
  }

  @Override
  public GroupRepresentation realmGroupByPathPathGet(String s, String s1) {
    return null;
  }

  @Override
  public void realmLogoutAllPost(String s) {

  }

  @Override
  public RealmRepresentation realmPartialExportPost(String s, Boolean aBoolean, Boolean aBoolean1) {
    return null;
  }

  @Override
  public RealmRepresentation realmPartialExportPost(String s, Map<String, Object> map) {
    return null;
  }

  @Override
  public void realmPartialImportPost(String s, PartialImportRepresentation partialImportRepresentation) {

  }

  @Override
  public void realmPushRevocationPost(String s) {

  }

  @Override
  public void realmPut(String s, RealmRepresentation realmRepresentation) {

  }

  @Override
  public void realmSessionsSessionDelete(String s, String s1) {

  }

  @Override
  public void realmTestLDAPConnectionPost(String s, TestLdapConnectionRepresentation testLdapConnectionRepresentation) {

  }

  @Override
  public void realmTestSMTPConnectionPost(String s, Map<String, Object> map) {

  }

  @Override
  public ManagementPermissionReference realmUsersManagementPermissionsGet(String s) {
    return null;
  }

  @Override
  public ManagementPermissionReference realmUsersManagementPermissionsPut(String s, ManagementPermissionReference managementPermissionReference) {
    return null;
  }

  @Override
  public void rootPost(RealmRepresentation realmRepresentation) {

  }
}
