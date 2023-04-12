package common.oauth2;

import it.cnr.iit.keycloak.api.UsersApi;
import it.cnr.iit.keycloak.model.ApiResponse;
import it.cnr.iit.keycloak.model.CredentialRepresentation;
import it.cnr.iit.keycloak.model.FederatedIdentityRepresentation;
import it.cnr.iit.keycloak.model.UserRepresentation;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Classe fallback implementa usersApi.
 *
 * @author Cristian
 *
 */
@Slf4j
@RequiredArgsConstructor
public class UsersApiFallback implements UsersApi {
  private final Exception exception;
  
  @Override
  public Integer realmUsersCountGet(String arg0, Map<String, Object> arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Integer realmUsersCountGet(String arg0, String arg1, Boolean arg2, String arg3,
      String arg4, String arg5, String arg6) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ApiResponse<Integer> realmUsersCountGetWithHttpInfo(String arg0,
      Map<String, Object> arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ApiResponse<Integer> realmUsersCountGetWithHttpInfo(String arg0, String arg1, Boolean arg2,
      String arg3, String arg4, String arg5, String arg6) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Map<String, Object>> realmUsersGet(String arg0, Map<String, Object> arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Map<String, Object>> realmUsersGet(String arg0, Boolean arg1, String arg2,
      Boolean arg3, Boolean arg4, Boolean arg5, Integer arg6, String arg7, String arg8, String arg9,
      String arg10, Integer arg11, String arg12, String arg13, String arg14) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ApiResponse<List<Map<String, Object>>> realmUsersGetWithHttpInfo(String arg0,
      Map<String, Object> arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ApiResponse<List<Map<String, Object>>> realmUsersGetWithHttpInfo(String arg0, Boolean arg1,
      String arg2, Boolean arg3, Boolean arg4, Boolean arg5, Integer arg6, String arg7, String arg8,
      String arg9, String arg10, Integer arg11, String arg12, String arg13, String arg14) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Map<String, Object>> realmUsersIdConfiguredUserStorageCredentialTypesGet(String arg0,
      String arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ApiResponse<List<Map<String, Object>>> 
      realmUsersIdConfiguredUserStorageCredentialTypesGetWithHttpInfo(
      String arg0, String arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void realmUsersIdConsentsClientDelete(String arg0, String arg1, String arg2) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ApiResponse<Void> realmUsersIdConsentsClientDeleteWithHttpInfo(String arg0, String arg1,
      String arg2) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Map<String, Object>> realmUsersIdConsentsGet(String arg0, String arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ApiResponse<List<Map<String, Object>>> realmUsersIdConsentsGetWithHttpInfo(String arg0,
      String arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void realmUsersIdCredentialsCredentialIdDelete(String arg0, String arg1, String arg2) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ApiResponse<Void> realmUsersIdCredentialsCredentialIdDeleteWithHttpInfo(String arg0,
      String arg1, String arg2) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void realmUsersIdCredentialsCredentialIdMoveAfterNewPreviousCredentialIdPost(String arg0,
      String arg1, String arg2, String arg3) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ApiResponse<Void> 
      realmUsersIdCredentialsCredentialIdMoveAfterNewPreviousCredentialIdPostWithHttpInfo(
      String arg0, String arg1, String arg2, String arg3) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void realmUsersIdCredentialsCredentialIdMoveToFirstPost(String arg0, String arg1,
      String arg2) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ApiResponse<Void> realmUsersIdCredentialsCredentialIdMoveToFirstPostWithHttpInfo(
      String arg0, String arg1, String arg2) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void realmUsersIdCredentialsCredentialIdUserLabelPut(String arg0, String arg1, String arg2,
      String arg3) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ApiResponse<Void> realmUsersIdCredentialsCredentialIdUserLabelPutWithHttpInfo(String arg0,
      String arg1, String arg2, String arg3) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Map<String, Object>> realmUsersIdCredentialsGet(String arg0, String arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ApiResponse<List<Map<String, Object>>> realmUsersIdCredentialsGetWithHttpInfo(String arg0,
      String arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void realmUsersIdDelete(String arg0, String arg1) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ApiResponse<Void> realmUsersIdDeleteWithHttpInfo(String arg0, String arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void realmUsersIdDisableCredentialTypesPut(String arg0, String arg1, List<String> arg2) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ApiResponse<Void> realmUsersIdDisableCredentialTypesPutWithHttpInfo(String arg0,
      String arg1, List<String> arg2) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void realmUsersIdExecuteActionsEmailPut(String arg0, String arg1, List<String> arg2,
      Map<String, Object> arg3) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void realmUsersIdExecuteActionsEmailPut(String arg0, String arg1, List<String> arg2,
      String arg3, Integer arg4, String arg5) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ApiResponse<Void> realmUsersIdExecuteActionsEmailPutWithHttpInfo(String arg0, String arg1,
      List<String> arg2, Map<String, Object> arg3) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ApiResponse<Void> realmUsersIdExecuteActionsEmailPutWithHttpInfo(String arg0, String arg1,
      List<String> arg2, String arg3, Integer arg4, String arg5) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Map<String, Object>> realmUsersIdFederatedIdentityGet(String arg0, String arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ApiResponse<List<Map<String, Object>>> realmUsersIdFederatedIdentityGetWithHttpInfo(
      String arg0, String arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void realmUsersIdFederatedIdentityProviderDelete(String arg0, String arg1, String arg2) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ApiResponse<Void> realmUsersIdFederatedIdentityProviderDeleteWithHttpInfo(String arg0,
      String arg1, String arg2) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void realmUsersIdFederatedIdentityProviderPost(String arg0, String arg1, String arg2,
      FederatedIdentityRepresentation arg3) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ApiResponse<Void> realmUsersIdFederatedIdentityProviderPostWithHttpInfo(String arg0,
      String arg1, String arg2, FederatedIdentityRepresentation arg3) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public UserRepresentation realmUsersIdGet(String s1, String s2) {
    log.error("get user by id \"{}\" from keycloak: {}", s1, exception.getMessage());
    return null;
  }

  @Override
  public ApiResponse<UserRepresentation> realmUsersIdGetWithHttpInfo(String arg0, String arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Map<String, Object> realmUsersIdGroupsCountGet(String arg0, String arg1, String arg2) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Map<String, Object> realmUsersIdGroupsCountGet(String arg0, String arg1,
      Map<String, Object> arg2) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ApiResponse<Map<String, Object>> realmUsersIdGroupsCountGetWithHttpInfo(String arg0,
      String arg1, String arg2) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ApiResponse<Map<String, Object>> realmUsersIdGroupsCountGetWithHttpInfo(String arg0,
      String arg1, Map<String, Object> arg2) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Map<String, Object>> realmUsersIdGroupsGet(String arg0, String arg1,
      Map<String, Object> arg2) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Map<String, Object>> realmUsersIdGroupsGet(String arg0, String arg1, Boolean arg2,
      Integer arg3, Integer arg4, String arg5) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ApiResponse<List<Map<String, Object>>> realmUsersIdGroupsGetWithHttpInfo(String arg0,
      String arg1, Map<String, Object> arg2) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ApiResponse<List<Map<String, Object>>> realmUsersIdGroupsGetWithHttpInfo(String arg0,
      String arg1, Boolean arg2, Integer arg3, Integer arg4, String arg5) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void realmUsersIdGroupsGroupIdDelete(String arg0, String arg1, String arg2) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ApiResponse<Void> realmUsersIdGroupsGroupIdDeleteWithHttpInfo(String arg0, String arg1,
      String arg2) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void realmUsersIdGroupsGroupIdPut(String arg0, String arg1, String arg2) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ApiResponse<Void> realmUsersIdGroupsGroupIdPutWithHttpInfo(String arg0, String arg1,
      String arg2) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Map<String, Object> realmUsersIdImpersonationPost(String arg0, String arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ApiResponse<Map<String, Object>> realmUsersIdImpersonationPostWithHttpInfo(String arg0,
      String arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void realmUsersIdLogoutPost(String arg0, String arg1) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ApiResponse<Void> realmUsersIdLogoutPostWithHttpInfo(String arg0, String arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Map<String, Object>> realmUsersIdOfflineSessionsClientUuidGet(String arg0,
      String arg1, String arg2) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ApiResponse<List<Map<String, Object>>> 
         realmUsersIdOfflineSessionsClientUuidGetWithHttpInfo(
      String arg0, String arg1, String arg2) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void realmUsersIdPut(String arg0, String arg1, UserRepresentation arg2) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ApiResponse<Void> realmUsersIdPutWithHttpInfo(String arg0, String arg1,
      UserRepresentation arg2) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void realmUsersIdResetPasswordPut(String arg0, String arg1,
      CredentialRepresentation arg2) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ApiResponse<Void> realmUsersIdResetPasswordPutWithHttpInfo(String arg0, String arg1,
      CredentialRepresentation arg2) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void realmUsersIdSendVerifyEmailPut(String arg0, String arg1, Map<String, Object> arg2) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void realmUsersIdSendVerifyEmailPut(String arg0, String arg1, String arg2, String arg3) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ApiResponse<Void> realmUsersIdSendVerifyEmailPutWithHttpInfo(String arg0, String arg1,
      Map<String, Object> arg2) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ApiResponse<Void> realmUsersIdSendVerifyEmailPutWithHttpInfo(String arg0, String arg1,
      String arg2, String arg3) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Map<String, Object>> realmUsersIdSessionsGet(String arg0, String arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ApiResponse<List<Map<String, Object>>> realmUsersIdSessionsGetWithHttpInfo(String arg0,
      String arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void realmUsersPost(String s, UserRepresentation userRepresentation) {
    log.warn("user creation on keycloak: {}, {}", userRepresentation, exception.getMessage());
  }

  @Override
  public ApiResponse<Void> realmUsersPostWithHttpInfo(String arg0, UserRepresentation arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String realmUsersProfileGet(String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ApiResponse<String> realmUsersProfileGetWithHttpInfo(String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void realmUsersProfilePut(String arg0, String arg1) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ApiResponse<Void> realmUsersProfilePutWithHttpInfo(String arg0, String arg1) {
    // TODO Auto-generated method stub
    return null;
  }

}
