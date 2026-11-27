package common.security;

import com.google.common.base.Optional;
import java.util.HashMap;
import manager.configurations.EpasParam;
import models.Attachment;
import models.Configuration;
import models.Office;
import models.Person;
import models.Qualification;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import models.enumerate.AccountRole;
import models.enumerate.AttachmentType;
import org.junit.Test;
import play.test.UnitTest;

/**
 * Precedenza dei dinieghi nelle regole dei permessi.
 *
 * <p>Le regole che chiamano revoke() devono prevalere sulle concessioni, qualunque sia l'ordine
 * in cui il motore esegue le regole: con Drools 9 l'ordine a parità di salience è diverso da
 * quello di Drools 5.6 (#698). I controlli passano per SecurityRules e per l'intero
 * permissions.drl, senza database.</p>
 */
public class DenyPrecedenceTest extends UnitTest {

  private static User user(Integer qualification, String... roleNames) {
    User user = new User();
    user.id = 900001L;
    user.setUsername("deny-precedence");
    Person person = new Person();
    person.id = 900002L;
    person.setUser(user);
    user.setPerson(person);
    Office office = new Office();
    office.id = 900003L;
    person.setOffice(office);
    Qualification q = new Qualification();
    q.id = 900004L;
    q.setQualification(qualification);
    person.setQualification(q);
    long id = 900010L;
    for (String roleName : roleNames) {
      Role role = new Role();
      role.id = id++;
      role.setName(roleName);
      UsersRolesOffices uro = new UsersRolesOffices();
      uro.id = id++;
      uro.setUser(user);
      uro.setRole(role);
      uro.setOffice(office);
      user.getUsersRolesOffices().add(uro);
    }
    return user;
  }

  private static boolean check(User user, String action, Object target) {
    SecurityRules rules = new SecurityRules(() -> Optional.of(user), () -> action,
        () -> SecureRulesPlugin.knowledgeBase, HashMap::new);
    return rules.check(action, target);
  }

  private static Attachment attachment(Office office, AttachmentType type) {
    Attachment attachment = new Attachment();
    attachment.id = 900030L;
    attachment.setOffice(office);
    attachment.setType(type);
    return attachment;
  }

  private static Configuration configuration(Office office, EpasParam param, String value) {
    Configuration configuration = new Configuration();
    configuration.id = 900031L;
    configuration.setOffice(office);
    configuration.setEpasParam(param);
    configuration.setFieldValue(value);
    return configuration;
  }

  @Test
  public void disabledUserIsDenied() {
    User user = user(5, Role.EMPLOYEE);
    user.setDisabled(true);
    assertFalse("Application.index", check(user, "Application.index", null));
    assertFalse("Stampings.stampings", check(user, "Stampings.stampings", null));
  }

  @Test
  public void enabledEmployeeCanSeeStampings() {
    assertTrue(check(user(5, Role.EMPLOYEE), "Stampings.stampings", null));
  }

  @Test
  public void autocertificationAttachmentCannotBeRemovedWhenEnabled() {
    User user = user(5, Role.PERSONNEL_ADMIN);
    Office office = user.getPerson().getOffice();
    office.getConfigurations().add(
        configuration(office, EpasParam.TR_AUTOCERTIFICATION, "true"));
    Attachment attachment = attachment(office, AttachmentType.TR_AUTOCERTIFICATION);
    assertFalse(check(user, "Configurations.removeAttachment", attachment));
    assertTrue(check(user, "Configurations.getAttachment", attachment));
  }

  @Test
  public void autocertificationAttachmentCanBeRemovedWhenDisabled() {
    User user = user(5, Role.PERSONNEL_ADMIN);
    Office office = user.getPerson().getOffice();
    office.getConfigurations().add(
        configuration(office, EpasParam.TR_AUTOCERTIFICATION, "false"));
    assertTrue(check(user, "Configurations.removeAttachment",
        attachment(office, AttachmentType.TR_AUTOCERTIFICATION)));
  }

  @Test
  public void technicianRecoveryLimitsCannotBeEdited() {
    User user = user(5, Role.PERSONNEL_ADMIN);
    Office office = user.getPerson().getOffice();
    for (EpasParam param : new EpasParam[] {
        EpasParam.MAX_RECOVERY_DAYS_49, EpasParam.MONTH_EXPIRY_RECOVERY_DAYS_49}) {
      Configuration configuration = configuration(office, param, "10");
      assertFalse(param.name(), check(user, "Configurations.edit", configuration));
      assertFalse(param.name(), check(user, "Configurations.update", configuration));
    }
  }

  @Test
  public void otherConfigurationsCanBeEdited() {
    User user = user(5, Role.PERSONNEL_ADMIN);
    Configuration configuration = configuration(
        user.getPerson().getOffice(), EpasParam.MAX_RECOVERY_DAYS_13, "10");
    assertTrue(check(user, "Configurations.update", configuration));
  }

  @Test
  public void overtimesDeniedToResearchersAndTechnologists() {
    assertFalse("qualifica 3", check(user(3, Role.EMPLOYEE), "CompetenceRequests.overtimes", null));
    assertTrue("qualifica 4", check(user(4, Role.EMPLOYEE), "CompetenceRequests.overtimes", null));
  }

  @Test
  public void adminAccountIsDeniedRevokedActions() {
    User user = user(5);
    user.getRoles().add(AccountRole.ADMIN);
    assertFalse(check(user, "Charts.excelFile", null));
    assertFalse(check(user, "Administration.emergency", null));
    assertTrue(check(user, "Administration.generalSetting", null));
  }
}
