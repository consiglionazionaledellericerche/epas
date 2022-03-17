package common.oauth2;

import it.cnr.iit.keycloak.api.RealmsAdminApi;
import it.cnr.iit.keycloak.model.*;
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
  public ApiResponse<Void> realmAdminEventsDeleteWithHttpInfo(String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Map<String, Object>> realmAdminEventsGet(String arg0, Map<String, Object> arg1) {
    return null;
  }

  @Override
  public List<Map<String, Object>> realmAdminEventsGet(String arg0, String arg1, String arg2,
      String arg3, String arg4, String arg5, String arg6, Integer arg7, Integer arg8,
      List<String> arg9, String arg10, List<String> arg11) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ApiResponse<List<Map<String, Object>>> realmAdminEventsGetWithHttpInfo(String arg0,
      Map<String, Object> arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ApiResponse<List<Map<String, Object>>> realmAdminEventsGetWithHttpInfo(String arg0,
      String arg1, String arg2, String arg3, String arg4, String arg5, String arg6, Integer arg7,
      Integer arg8, List<String> arg9, String arg10, List<String> arg11) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void realmClearKeysCachePost(String s) {
  }

  @Override
  public ApiResponse<Void> realmClearKeysCachePostWithHttpInfo(String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void realmClearRealmCachePost(String s) {
  }

  @Override
  public ApiResponse<Void> realmClearRealmCachePostWithHttpInfo(String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void realmClearUserCachePost(String s) {
  }

  @Override
  public ApiResponse<Void> realmClearUserCachePostWithHttpInfo(String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ClientRepresentation realmClientDescriptionConverterPost(String arg0, String arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ApiResponse<ClientRepresentation> realmClientDescriptionConverterPostWithHttpInfo(
      String arg0, String arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ClientPoliciesRepresentation realmClientPoliciesPoliciesGet(String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ApiResponse<ClientPoliciesRepresentation> realmClientPoliciesPoliciesGetWithHttpInfo(
      String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void realmClientPoliciesPoliciesPut(String arg0, ClientPoliciesRepresentation arg1) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ApiResponse<Void> realmClientPoliciesPoliciesPutWithHttpInfo(String arg0,
      ClientPoliciesRepresentation arg1) {
    return null;
  }

  @Override
  public ClientProfilesRepresentation realmClientPoliciesProfilesGet(String arg0, Boolean arg1) {
    return null;
  }

  @Override
  public ClientProfilesRepresentation realmClientPoliciesProfilesGet(String arg0,
      Map<String, Object> arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ApiResponse<ClientProfilesRepresentation> realmClientPoliciesProfilesGetWithHttpInfo(
      String arg0, Boolean arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ApiResponse<ClientProfilesRepresentation> realmClientPoliciesProfilesGetWithHttpInfo(
      String arg0, Map<String, Object> arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void realmClientPoliciesProfilesPut(String arg0, ClientProfilesRepresentation arg1) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ApiResponse<Void> realmClientPoliciesProfilesPutWithHttpInfo(String arg0,
      ClientProfilesRepresentation arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Map<String, Object>> realmClientSessionStatsGet(String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ApiResponse<List<Map<String, Object>>> realmClientSessionStatsGetWithHttpInfo(
      String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Map<String, Object>> realmCredentialRegistratorsGet(String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ApiResponse<List<Map<String, Object>>> realmCredentialRegistratorsGetWithHttpInfo(
      String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void realmDefaultDefaultClientScopesClientScopeIdDelete(String arg0, String arg1) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ApiResponse<Void> realmDefaultDefaultClientScopesClientScopeIdDeleteWithHttpInfo(
      String arg0, String arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void realmDefaultDefaultClientScopesClientScopeIdPut(String arg0, String arg1) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ApiResponse<Void> realmDefaultDefaultClientScopesClientScopeIdPutWithHttpInfo(String arg0,
      String arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Map<String, Object>> realmDefaultDefaultClientScopesGet(String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ApiResponse<List<Map<String, Object>>> realmDefaultDefaultClientScopesGetWithHttpInfo(
      String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Map<String, Object>> realmDefaultGroupsGet(String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ApiResponse<List<Map<String, Object>>> realmDefaultGroupsGetWithHttpInfo(String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void realmDefaultGroupsGroupIdDelete(String arg0, String arg1) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ApiResponse<Void> realmDefaultGroupsGroupIdDeleteWithHttpInfo(String arg0, String arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void realmDefaultGroupsGroupIdPut(String arg0, String arg1) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ApiResponse<Void> realmDefaultGroupsGroupIdPutWithHttpInfo(String arg0, String arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void realmDefaultOptionalClientScopesClientScopeIdDelete(String arg0, String arg1) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ApiResponse<Void> realmDefaultOptionalClientScopesClientScopeIdDeleteWithHttpInfo(
      String arg0, String arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void realmDefaultOptionalClientScopesClientScopeIdPut(String arg0, String arg1) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ApiResponse<Void> realmDefaultOptionalClientScopesClientScopeIdPutWithHttpInfo(String arg0,
      String arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Map<String, Object>> realmDefaultOptionalClientScopesGet(String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ApiResponse<List<Map<String, Object>>> realmDefaultOptionalClientScopesGetWithHttpInfo(
      String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void realmDelete(String arg0) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ApiResponse<Void> realmDeleteWithHttpInfo(String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public RealmEventsConfigRepresentation realmEventsConfigGet(String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ApiResponse<RealmEventsConfigRepresentation> realmEventsConfigGetWithHttpInfo(
      String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void realmEventsConfigPut(String arg0, RealmEventsConfigRepresentation arg1) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ApiResponse<Void> realmEventsConfigPutWithHttpInfo(String arg0,
      RealmEventsConfigRepresentation arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void realmEventsDelete(String arg0) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ApiResponse<Void> realmEventsDeleteWithHttpInfo(String arg0) {
    return null;
  }

  @Override
  public List<Map<String, Object>> realmEventsGet(String s, Map<String, Object> map) {
    log.error("realm-admin-api.realmEventsGet({}, {})", s, map, exception);
    return List.of();
  }

  @Override
  public List<Map<String, Object>> realmEventsGet(String arg0, String arg1, String arg2,
      String arg3, Integer arg4, String arg5, Integer arg6, List<String> arg7, String arg8) {
    return null;
  }

  @Override
  public ApiResponse<List<Map<String, Object>>> realmEventsGetWithHttpInfo(String arg0,
      Map<String, Object> arg1) {
    return null;
  }

  @Override
  public ApiResponse<List<Map<String, Object>>> realmEventsGetWithHttpInfo(String arg0, String arg1,
      String arg2, String arg3, Integer arg4, String arg5, Integer arg6, List<String> arg7,
      String arg8) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public RealmRepresentation realmGet(String arg0) {
    return null;
  }

  @Override
  public ApiResponse<RealmRepresentation> realmGetWithHttpInfo(String arg0) {
    return null;
  }

  @Override
  public GroupRepresentation realmGroupByPathPathGet(String arg0, String arg1) {
    return null;
  }

  @Override
  public ApiResponse<GroupRepresentation> realmGroupByPathPathGetWithHttpInfo(String arg0,
      String arg1) {
    return null;
  }

  @Override
  public void realmLdapServerCapabilitiesPost(String s, TestLdapConnectionRepresentation arg1) {
  }

  @Override
  public ApiResponse<Void> realmLdapServerCapabilitiesPostWithHttpInfo(String s,
      TestLdapConnectionRepresentation arg1) {
    return null;
  }

  @Override
  public List<Map<String, Object>> realmLocalizationGet(String arg0) {
    return List.of();
  }

  @Override
  public ApiResponse<List<Map<String, Object>>> realmLocalizationGetWithHttpInfo(String arg0) {
    return null;
  }

  @Override
  public void realmLocalizationLocaleDelete(String arg0, String arg1) {    
  }

  @Override
  public ApiResponse<Void> realmLocalizationLocaleDeleteWithHttpInfo(String arg0, String arg1) {
    return null;
  }

  @Override
  public Map<String, Object> realmLocalizationLocaleGet(String arg0, String arg1) {
    return Map.of();
  }

  @Override
  public ApiResponse<Map<String, Object>> realmLocalizationLocaleGetWithHttpInfo(String arg0,
      String arg1) {
    return null;
  }

  @Override
  public void realmLocalizationLocaleKeyDelete(String arg0, String arg1, String arg2) {    
  }

  @Override
  public ApiResponse<Void> realmLocalizationLocaleKeyDeleteWithHttpInfo(String arg0, String arg1,
      String arg2) {
    return null;
  }

  @Override
  public String realmLocalizationLocaleKeyGet(String arg0, String arg1, String arg2) {
    return null;
  }

  @Override
  public ApiResponse<String> realmLocalizationLocaleKeyGetWithHttpInfo(String arg0, String arg1,
      String arg2) {
    return null;
  }

  @Override
  public void realmLocalizationLocaleKeyPut(String arg0, String arg1, String arg2, String arg3) {
  }

  @Override
  public ApiResponse<Void> realmLocalizationLocaleKeyPutWithHttpInfo(String arg0, String arg1,
      String arg2, String arg3) {
    return null;
  }

  @Override
  public void realmLocalizationLocalePost(String arg0, String arg1, Map<String, Object> arg2) {
  }

  @Override
  public ApiResponse<Void> realmLocalizationLocalePostWithHttpInfo(String arg0, String arg1,
      Map<String, Object> arg2) {
    return null;
  }

  @Override
  public GlobalRequestResult realmLogoutAllPost(String arg0) {
    return null;
  }

  @Override
  public ApiResponse<GlobalRequestResult> realmLogoutAllPostWithHttpInfo(String arg0) {
    return null;
  }

  @Override
  public RealmRepresentation realmPartialExportPost(String arg0, Map<String, Object> arg1) {
    return null;
  }

  @Override
  public RealmRepresentation realmPartialExportPost(String arg0, Boolean arg1, Boolean arg2) {
    return null;
  }

  @Override
  public ApiResponse<RealmRepresentation> realmPartialExportPostWithHttpInfo(String arg0,
      Map<String, Object> arg1) {
    return null;
  }

  @Override
  public ApiResponse<RealmRepresentation> realmPartialExportPostWithHttpInfo(String arg0,
      Boolean arg1, Boolean arg2) {
    return null;
  }

  @Override
  public void realmPartialImportPost(String arg0, PartialImportRepresentation arg1) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ApiResponse<Void> realmPartialImportPostWithHttpInfo(String arg0,
      PartialImportRepresentation arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void realmPushRevocationPost(String arg0) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ApiResponse<Void> realmPushRevocationPostWithHttpInfo(String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void realmPut(String arg0, RealmRepresentation arg1) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ApiResponse<Void> realmPutWithHttpInfo(String arg0, RealmRepresentation arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void realmSessionsSessionDelete(String arg0, String arg1) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ApiResponse<Void> realmSessionsSessionDeleteWithHttpInfo(String arg0, String arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void realmTestLDAPConnectionPost(String arg0, TestLdapConnectionRepresentation arg1) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ApiResponse<Void> realmTestLDAPConnectionPostWithHttpInfo(String arg0,
      TestLdapConnectionRepresentation arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void realmTestSMTPConnectionPost(String arg0, Map<String, Object> arg1) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ApiResponse<Void> realmTestSMTPConnectionPostWithHttpInfo(String arg0,
      Map<String, Object> arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ManagementPermissionReference realmUsersManagementPermissionsGet(String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ApiResponse<ManagementPermissionReference> realmUsersManagementPermissionsGetWithHttpInfo(
      String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ManagementPermissionReference realmUsersManagementPermissionsPut(String arg0,
      ManagementPermissionReference arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ApiResponse<ManagementPermissionReference> realmUsersManagementPermissionsPutWithHttpInfo(
      String arg0, ManagementPermissionReference arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void rootPost(RealmRepresentation arg0) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ApiResponse<Void> rootPostWithHttpInfo(RealmRepresentation arg0) {
    // TODO Auto-generated method stub
    return null;
  }

}
