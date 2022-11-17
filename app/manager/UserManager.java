/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package manager;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import dao.UserDao;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import models.Office;
import models.Person;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import org.joda.time.LocalDate;
import play.libs.Codec;

/**
 * Manager per user.
 *
 * @author Daniele Murgia
 * @since 13/10/15
 */
@Slf4j
public class UserManager {

  private final UserDao userDao;

  /**
   * Construttore per l'injection.
   */
  @Inject
  public UserManager(UserDao userDao) {
    this.userDao = userDao;
  }

  /**
   * Return generated token for the recovery password procedure.
   *
   * @param person person for which to generate the token.
   */
  public void generateRecoveryToken(Person person) {

    Preconditions.checkState(person != null && person.isPersistent());

    //generate random token
    SecureRandom random = new SecureRandom();

    person.getUser().setRecoveryToken(new BigInteger(130, random).toString(32));
    person.getUser().setExpireRecoveryToken(LocalDate.now());
    person.getUser().save();
  }

  /**
   * Return generated username using pattern 'name.surname'.
   *
   * @param name    Name
   * @param surname Surname
   * @return generated Username
   */
  public String generateUserName(final String name, final String surname) {

    final String username;
    final String standardUsername = CharMatcher.whitespace().removeFrom(
        Joiner.on(".").skipNulls().join(name.replaceAll("\\W", ""), surname.replaceAll("\\W", ""))
            .toLowerCase());

    List<String> overlapUsers = userDao.containsUsername(standardUsername);
    //  Caso standard
    if (overlapUsers.isEmpty()) {
      username = standardUsername;
    } else {

      //  Caso di omonimia

      //  Cerco tutti i numeri della sequenza autogenerata per i casi di omonimia
      List<Integer> sequence = Lists.newArrayList();
      for (String user : overlapUsers) {
        String number = user.replaceAll("\\D+", "");
        if (!Strings.isNullOrEmpty(number)) {
          sequence.add(Integer.parseInt(number));
        }
      }
      //  Solo un omonimo
      if (sequence.isEmpty()) {
        username = standardUsername + 1;
      } else {
        //  Più di un omonimo
        username = standardUsername + (Collections.max(sequence) + 1);
      }
    }
    return username;
  }

  /**
   * Crea l'utente.
   *
   * @param person la persona per cui creare l'utente
   * @return l'utente creato.
   */
  public User createUser(final Person person) {

    User user = new User();

    user.setUsername(generateUserName(person.getName(), person.getSurname()));

    SecureRandom random = new SecureRandom();
    user.setPassword(Codec.hexMD5(new BigInteger(130, random).toString(32)));

    user.save();

    person.setUser(user);

    log.info("Creato nuovo user per {}: username = {}", person.fullName(), user.getUsername());

    return user;
  }

  /**
   * funzione che salva l'utente e genere i ruoli sugli uffici.
   *
   * @param user    l'user da salvare
   * @param offices la lista degli uffici
   * @param roles   la lista dei ruoli
   * @param enable  se deve essere disabilitato
   */
  public void saveUser(User user, Set<Office> offices, Set<Role> roles, boolean enable) {
    user.setPassword(Codec.hexMD5(user.getPassword()));
    if (enable) {
      user.setDisabled(false);
      user.setExpireDate(null);
    }
    user.save();
    for (Role role : roles) {
      for (Office office : offices) {
        UsersRolesOffices uro = new UsersRolesOffices();
        uro.setUser(user);
        uro.setOffice(office);
        uro.setRole(role);
        uro.save();
      }
    }

  }

}