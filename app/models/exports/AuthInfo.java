/**
 *
 */
package models.exports;

/**
 * Classe utilizzata per passare via JSON i dati relativi all'autenticazione
 *
 * @author cristian
 */
public class AuthInfo {

  private final String username;
  private final String password;

  public AuthInfo(String username, String password) {
    this.username = username;
    this.password = password;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

}
