package common.oauth2;

import it.cnr.iit.keycloak.api.UsersApi;
import it.cnr.iit.keycloak.model.CredentialRepresentation;
import it.cnr.iit.keycloak.model.UserRepresentation;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import models.User;

@Singleton
@Slf4j
public class UserManager {

  private final UsersApi usersApi;
  private final String realm;

  @Inject
  public UserManager(@NonNull UsersApi usersApi,
                     @NonNull @Named(OpenIdClientsModule.KEYCLOAK_REALM) String realm) {
    this.usersApi = usersApi;
    this.realm = realm;
  }

  /**
   *
   * @param person l'utente da cercare (via keycloak id)
   * @return l'id dello user se c'è
   */
  public Optional<UserRepresentation> find(User user) {
    return Optional.ofNullable(user.keycloakId).map(id -> usersApi.realmUsersIdGet(realm, id));
  }

  Map<String, Object> byUsername(User user) {
    return usersApi.realmUsersGet(realm, Map.of("username", user.username))
        .stream().findFirst().orElseThrow();
  }

  public void updatePassword(User user, String password) {
    log.info("update keycloak credentials with legacy password for {}", user);
    val data = new CredentialRepresentation();
    data.setTemporary(false);
    data.setValue(password);
    data.setType("password");
    usersApi.realmUsersIdResetPasswordPut(realm, user.keycloakId, data);
  }


  public Integer usersCount() {
    return usersApi.realmUsersCountGet(realm, Map.of());
  }

  /**
   * @param date
   * @return la localdatetime convertita
   */
  public static LocalDateTime toLocalDateTime(Instant date) {
    return date.atZone(ZoneId.systemDefault()).toLocalDateTime();
  }
}
