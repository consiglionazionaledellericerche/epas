package common.oauth2;

import com.google.common.base.Verify;
import it.besmartbeopen.keycloak.api.RealmsAdminApi;
import it.besmartbeopen.keycloak.api.UsersApi;
import it.besmartbeopen.keycloak.model.CredentialRepresentation;
import it.besmartbeopen.keycloak.model.UserRepresentation;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import models.Person;
import models.User;

@Singleton
@Slf4j
public class UserManager {

  private final UsersApi usersApi;
  private final RealmsAdminApi adminApi;
  private final String realm;
  private final String client;

  @Inject
  public UserManager(@NonNull UsersApi usersApi,
                     @NonNull RealmsAdminApi adminApi,
                     @NonNull @Named(OpenIdClientsModule.KEYCLOAK_REALM) String realm,
                     @NonNull @Named(OpenIdClientsModule.KEYCLOAK_CLIENT_ID) String client) {
    this.usersApi = usersApi;
    this.adminApi = adminApi;
    this.realm = realm;
    this.client = client;
  }

  /**
   *
   * @param person l'utente da cercare (via keycloak id)
   * @return l'id dello user se c'è
   */
  public Optional<UserRepresentation> find(User user) {
    return Optional.ofNullable(user.keycloakId).map(id -> usersApi.realmUsersIdGet(realm, id));
  }

  UserRepresentation byUsername(User user) {
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

  /**
   * Occorre: il ruolo realm-management -> view-events
   *
   * @param user l'operatore di cui individuare l'ultimo accesso
   * @param num quanti accessi plevare (>=1)
   * @return le eventuali informazioni sugli ultimi accessi
   */
  public List<AccessInfo> lastLogin(@NonNull User user, int num) {
    Verify.verify(num >= 1);
    if (user.keycloakId == null) {
      log.warn("ignoring non keycloak user {}", user);
      return null;
    }
    return adminApi.realmEventsGet(realm,
        Map.of("type", "LOGIN","client", client, "max", num,
            "user", user.keycloakId)).stream()
        .map(item -> new AccessInfo(toLocalDateTime(Instant.ofEpochMilli(item.getTime())),
            item.getIpAddress()))
        .collect(Collectors.toList());
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
