package models.exports;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Classe utilizzata per passare via JSON i dati relativi all'autenticazione.
 *
 * @author cristian
 */
@Getter
@RequiredArgsConstructor
public class AuthInfo {

  private final String username;
  private final String password;

}
