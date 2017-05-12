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
 * @author daniele
 * @since 13/10/15.
 */
@Slf4j
public class UserManager {

  private final UserDao userDao;

  @Inject
  public UserManager(UserDao userDao) {
    this.userDao = userDao;
  }

  /**
   * Return generated token for the recovery password procedure
   *
   * @param person person for which to generate the token.
   */
  public void generateRecoveryToken(Person person) {

    Preconditions.checkState(person != null && person.isPersistent());

    //generate random token
    SecureRandom random = new SecureRandom();

    person.user.recoveryToken = new BigInteger(130, random).toString(32);
    person.user.expireRecoveryToken = LocalDate.now();
    person.user.save();
  }

  /**
   * Return generated username using pattern 'name.surname'
   *
   * @param name    Name
   * @param surname Surname
   * @return generated Username
   */
  public String generateUserName(final String name, final String surname) {

    final String username;
    final String standardUsername = CharMatcher.WHITESPACE.removeFrom(
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

  public User createUser(final Person person) {

    User user = new User();

    user.username = generateUserName(person.name, person.surname);

    SecureRandom random = new SecureRandom();
    user.password = Codec.hexMD5(new BigInteger(130, random).toString(32));

    user.save();

    person.user = user;

    log.info("Creato nuovo user per {}: username = {}", person.fullName(), user.username);

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
    user.password = Codec.hexMD5(user.password);
    if (enable) {
      user.disabled = false;
      user.expireDate = null;
    }
    user.save();
    for (Role role : roles) {
      for (Office office : offices) {
        UsersRolesOffices uro = new UsersRolesOffices();
        uro.user = user;
        uro.office = office;
        uro.role = role;
        uro.save();
      }
    }

  }

}
