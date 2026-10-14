/*
 * Copyright (C) 2026  Consiglio Nazionale delle Ricerche
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

package manager.flows;

import com.google.common.base.Optional;
import java.time.LocalDate;
import models.Office;
import models.Person;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import models.flows.AbsenceRequest;
import models.flows.Affiliation;
import models.flows.CompetenceRequest;
import models.flows.Group;
import models.flows.enumerate.CompetenceRequestType;
import models.informationrequests.IllnessRequest;
import org.junit.Before;
import org.junit.Test;
import play.test.UnitTest;

/**
 * Verifica che chi approva o respinge una richiesta abbia il ruolo sulla sede o sul gruppo
 * della persona che l'ha effettuata.
 */
public class FlowApproversTest extends UnitTest {

  private AbsenceRequestManager absenceRequestManager;
  private CompetenceRequestManager competenceRequestManager;
  private InformationRequestManager informationRequestManager;

  private Office officeA;
  private Office officeB;
  private Person requester;

  /**
   * Crea i manager senza dipendenze e una persona della sede A.
   */
  @Before
  public void setUp() {
    absenceRequestManager = new AbsenceRequestManager(null, null, null, null, null, null, null,
        null, null, null, null, null, null, null);
    competenceRequestManager = new CompetenceRequestManager(null, null, null, null, null, null,
        null, null, null, null, null, null, null, null);
    informationRequestManager = new InformationRequestManager(null, null, null, null, null);
    officeA = new Office();
    officeB = new Office();
    requester = new Person();
    requester.setOffice(officeA);
  }

  private User userWithRole(Office office, String roleName) {
    User user = new User();
    Person person = new Person();
    person.setOffice(office);
    user.setPerson(person);
    addRole(user, office, roleName);
    return user;
  }

  private void addRole(User user, Office office, String roleName) {
    Role role = new Role();
    role.setName(roleName);
    UsersRolesOffices uro = new UsersRolesOffices();
    uro.setUser(user);
    uro.setOffice(office);
    uro.setRole(role);
    user.getUsersRolesOffices().add(uro);
  }

  private void addToGroupOf(User manager, Person person, LocalDate endDate) {
    Group group = new Group();
    group.setManager(manager.getPerson());
    Affiliation affiliation = new Affiliation();
    affiliation.setGroup(group);
    affiliation.setPerson(person);
    affiliation.setBeginDate(LocalDate.now().minusYears(1));
    affiliation.setEndDate(endDate);
    group.getAffiliations().add(affiliation);
    manager.getPerson().getGroupsPeople().add(group);
  }

  private AbsenceRequest absenceRequest() {
    AbsenceRequest request = new AbsenceRequest();
    request.setPerson(requester);
    return request;
  }

  @Test
  public void rolesAreCheckedOnTheRequesterOffice() {
    User sameOffice = userWithRole(officeA, Role.SEAT_SUPERVISOR);
    User otherOffice = userWithRole(officeB, Role.SEAT_SUPERVISOR);

    assertTrue(sameOffice.hasRolesOnOffice(officeA, Role.SEAT_SUPERVISOR));
    assertFalse(otherOffice.hasRolesOnOffice(officeA, Role.SEAT_SUPERVISOR));
    assertFalse(sameOffice.hasRolesOnOffice(officeA, Role.PERSONNEL_ADMIN));
    assertFalse(sameOffice.hasRolesOnOffice(null, Role.SEAT_SUPERVISOR));
  }

  @Test
  public void groupManagerOnlyForCurrentMembersOfOwnGroups() {
    User manager = userWithRole(officeA, Role.GROUP_MANAGER);
    Person member = new Person();
    Person formerMember = new Person();
    addToGroupOf(manager, member, null);
    addToGroupOf(manager, formerMember, LocalDate.now().minusDays(1));

    assertTrue(manager.isGroupManagerOf(member));
    assertFalse(manager.isGroupManagerOf(formerMember));
    assertFalse(manager.isGroupManagerOf(requester));
    assertFalse(manager.isGroupManagerOf(null));

    User employee = userWithRole(officeA, Role.EMPLOYEE);
    addToGroupOf(employee, member, null);
    assertFalse("serve anche il ruolo di responsabile di gruppo",
        employee.isGroupManagerOf(member));
  }

  @Test
  public void absenceRequestApprovers() {
    AbsenceRequest request = absenceRequest();

    assertTrue(absenceRequestManager.isApprover(request,
        userWithRole(officeA, Role.SEAT_SUPERVISOR)));
    assertTrue(absenceRequestManager.isApprover(request,
        userWithRole(officeA, Role.PERSONNEL_ADMIN)));
    assertFalse(absenceRequestManager.isApprover(request,
        userWithRole(officeB, Role.SEAT_SUPERVISOR)));
    assertFalse(absenceRequestManager.isApprover(request,
        userWithRole(officeB, Role.PERSONNEL_ADMIN)));
    assertFalse(absenceRequestManager.isApprover(request,
        userWithRole(officeA, Role.GROUP_MANAGER)));
    assertFalse(absenceRequestManager.isApprover(request,
        userWithRole(officeA, Role.EMPLOYEE)));

    User manager = userWithRole(officeB, Role.GROUP_MANAGER);
    addToGroupOf(manager, requester, null);
    assertTrue(absenceRequestManager.isApprover(request, manager));
  }

  @Test
  public void seatSupervisorOfAnotherOfficeCannotApproveAbsences() {
    AbsenceRequest request = absenceRequest();
    request.setOfficeHeadApprovalRequired(true);

    assertFalse(absenceRequestManager.approval(request,
        userWithRole(officeB, Role.SEAT_SUPERVISOR)));
    assertFalse(absenceRequestManager.approval(request,
        userWithRole(officeB, Role.PERSONNEL_ADMIN)));
    assertNull(request.getOfficeHeadApproved());
  }

  @Test
  public void informationRequestApprovers() {
    IllnessRequest request = new IllnessRequest();
    request.setPerson(requester);
    request.setOfficeHeadApprovalRequired(true);
    request.setAdministrativeApprovalRequired(true);

    assertTrue(informationRequestManager.isApprover(request,
        userWithRole(officeA, Role.SEAT_SUPERVISOR)));
    assertTrue(informationRequestManager.isApprover(request,
        userWithRole(officeA, Role.PERSONNEL_ADMIN)));
    assertFalse(informationRequestManager.isApprover(request,
        userWithRole(officeB, Role.SEAT_SUPERVISOR)));
    assertFalse(informationRequestManager.isApprover(request,
        userWithRole(officeB, Role.PERSONNEL_ADMIN)));

    assertFalse(informationRequestManager.approval(Optional.absent(),
        Optional.of(request), Optional.absent(),
        Optional.absent(), userWithRole(officeB, Role.SEAT_SUPERVISOR)));
    assertNull(request.getOfficeHeadApproved());
  }

  @Test
  public void onlyTheTeamMateApprovesAReperibilityChange() {
    CompetenceRequest request = new CompetenceRequest();
    request.setType(CompetenceRequestType.CHANGE_REPERIBILITY_REQUEST);
    request.setPerson(requester);
    request.setTeamMate(new Person());
    request.setEmployeeApprovalRequired(true);
    request.setManagerApprovalRequired(false);
    request.setOfficeHeadApprovalRequired(false);

    assertFalse(competenceRequestManager.approval(request,
        userWithRole(officeA, Role.EMPLOYEE)));
    assertNull(request.getEmployeeApproved());
  }

  @Test
  public void seatSupervisorOfAnotherOfficeCannotApproveCompetences() {
    CompetenceRequest request = new CompetenceRequest();
    request.setType(CompetenceRequestType.OVERTIME_REQUEST);
    request.setPerson(requester);
    request.setEmployeeApprovalRequired(false);
    request.setManagerApprovalRequired(false);
    request.setOfficeHeadApprovalRequired(true);

    assertFalse(competenceRequestManager.approval(request,
        userWithRole(officeB, Role.SEAT_SUPERVISOR)));
    assertFalse("il ramo del responsabile di sede richiede il ruolo",
        competenceRequestManager.approval(request, userWithRole(officeA, Role.EMPLOYEE)));
    assertNull(request.getOfficeHeadApproved());
  }
}
