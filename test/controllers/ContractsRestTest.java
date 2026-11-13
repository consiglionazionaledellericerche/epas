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

package controllers;

import common.injection.StaticInject;
import db.h2support.H2Examples;
import java.util.List;
import javax.inject.Inject;
import models.Contract;
import models.Office;
import models.Person;
import models.Role;
import models.User;
import models.enumerate.ContractType;
import org.joda.time.LocalDate;
import org.junit.Test;
import play.db.jpa.JPA;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.test.FunctionalTest;

/**
 * Test funzionali sulla creazione e sulla modifica dei contratti via REST (v2): la tipologia
 * del contratto è facoltativa e, se manca, è ricavata dal campo onCertificate.
 */
@StaticInject
public class ContractsRestTest extends FunctionalTest {

  private static final String CREATE_URL = "/rest/v2/contracts/create";
  private static final String BEGIN_DATE = "2026-01-01";

  @Inject
  private static H2Examples h2Examples;

  /**
   * Senza tipologia il contratto è creato come dipendente strutturato, presente negli
   * attestati.
   */
  @Test
  public void createWithoutContractTypeUsesStructuredAsDefault() {
    Fixture fixture = fixture();
    Response response = POST(manager(fixture), CREATE_URL, "application/json",
        "{\"personId\": " + fixture.person.id + ", \"beginDate\": \"" + BEGIN_DATE + "\"}");
    assertIsOk(response);
    assertTrue(getContent(response)
        .contains("\"contractType\":\"structured_public_administration\""));

    Contract contract = onlyContract(fixture.person);
    assertEquals(ContractType.structured_public_administration, contract.getContractType());
    assertTrue(contract.isOnCertificate());
    assertEquals(new LocalDate(2026, 1, 1), contract.getBeginDate());
  }

  /**
   * La tipologia indicata è rispettata e determina la presenza negli attestati.
   */
  @Test
  public void createWithContractType() {
    Fixture fixture = fixture();
    assertIsOk(POST(manager(fixture), CREATE_URL, "application/json",
        "{\"personId\": " + fixture.person.id + ", \"beginDate\": \"" + BEGIN_DATE
        + "\", \"contractType\": \"interim\"}"));

    Contract contract = onlyContract(fixture.person);
    assertEquals(ContractType.interim, contract.getContractType());
    assertFalse(contract.isOnCertificate());
  }

  /**
   * I client che usano ancora onCertificate=false ottengono un contratto non strutturato,
   * come i contratti esistenti quando è stata introdotta la tipologia.
   */
  @Test
  public void createWithOnCertificateFalseIsUnstructured() {
    Fixture fixture = fixture();
    assertIsOk(POST(manager(fixture), CREATE_URL, "application/json",
        "{\"personId\": " + fixture.person.id + ", \"beginDate\": \"" + BEGIN_DATE
        + "\", \"onCertificate\": false}"));

    Contract contract = onlyContract(fixture.person);
    assertEquals(ContractType.unstructured, contract.getContractType());
    assertFalse(contract.isOnCertificate());
  }

  /**
   * Una tipologia sconosciuta è rifiutata: non viene sostituita con quella predefinita.
   */
  @Test
  public void createWithUnknownContractTypeIsRejected() {
    Fixture fixture = fixture();
    Response response = POST(manager(fixture), CREATE_URL, "application/json",
        "{\"personId\": " + fixture.person.id + ", \"beginDate\": \"" + BEGIN_DATE
        + "\", \"contractType\": \"interinale\"}");
    assertStatus(400, response);
    assertTrue(getContent(response).contains("contractType"));
    assertTrue(contracts(fixture.person).isEmpty());
  }

  /**
   * Tipologia e onCertificate in contraddizione sono rifiutati, coerenti sono accettati.
   */
  @Test
  public void createWithContractTypeAndOnCertificate() {
    Fixture fixture = fixture();
    Response response = POST(manager(fixture), CREATE_URL, "application/json",
        "{\"personId\": " + fixture.person.id + ", \"beginDate\": \"" + BEGIN_DATE
        + "\", \"contractType\": \"interim\", \"onCertificate\": true}");
    assertStatus(400, response);
    assertTrue(getContent(response).contains("onCertificate"));
    assertStatus(400, POST(manager(fixture), CREATE_URL, "application/json",
        "{\"personId\": " + fixture.person.id + ", \"beginDate\": \"" + BEGIN_DATE
        + "\", \"contractType\": \"structured_public_administration\", "
        + "\"onCertificate\": false}"));
    assertTrue(contracts(fixture.person).isEmpty());

    assertIsOk(POST(manager(fixture), CREATE_URL, "application/json",
        "{\"personId\": " + fixture.person.id + ", \"beginDate\": \"" + BEGIN_DATE
        + "\", \"contractType\": \"interim\", \"onCertificate\": false}"));
    assertEquals(ContractType.interim, onlyContract(fixture.person).getContractType());
  }

  /**
   * Un body che non è un oggetto JSON è rifiutato con 400.
   */
  @Test
  public void createWithMalformedBodyIsRejected() {
    Fixture fixture = fixture();
    assertStatus(400, POST(manager(fixture), CREATE_URL, "application/json", "[1, 2]"));
    assertStatus(400, POST(manager(fixture), CREATE_URL, "application/json", "{\"personId\": "));
    assertTrue(contracts(fixture.person).isEmpty());
  }

  /**
   * Nella modifica il solo onCertificate cambia la tipologia quando è diverso da quello
   * attuale del contratto; se è uguale la tipologia resta (un interinale resta interinale).
   */
  @Test
  public void updateWithOnlyOnCertificate() {
    Fixture fixture = fixture();
    assertIsOk(POST(manager(fixture), CREATE_URL, "application/json",
        "{\"personId\": " + fixture.person.id + ", \"beginDate\": \"" + BEGIN_DATE
        + "\", \"contractType\": \"interim\"}"));
    String updateUrl = "/rest/v2/contracts/update?id=" + onlyContract(fixture.person).id;
    String common = "{\"personId\": " + fixture.person.id + ", \"beginDate\": \""
        + BEGIN_DATE + "\", ";

    assertIsOk(PUT(manager(fixture), updateUrl, "application/json",
        common + "\"onCertificate\": false}"));
    assertEquals(ContractType.interim, onlyContract(fixture.person).getContractType());

    assertIsOk(PUT(manager(fixture), updateUrl, "application/json",
        common + "\"onCertificate\": true}"));
    Contract contract = onlyContract(fixture.person);
    assertEquals(ContractType.structured_public_administration, contract.getContractType());
    assertTrue(contract.isOnCertificate());

    assertIsOk(PUT(manager(fixture), updateUrl, "application/json",
        common + "\"onCertificate\": false}"));
    contract = onlyContract(fixture.person);
    assertEquals(ContractType.unstructured, contract.getContractType());
    assertFalse(contract.isOnCertificate());

    assertStatus(400, PUT(manager(fixture), updateUrl, "application/json",
        common + "\"contractType\": \"interim\", \"onCertificate\": true}"));
    assertEquals(ContractType.unstructured, onlyContract(fixture.person).getContractType());
  }

  /**
   * La modifica cambia la tipologia solo quando è indicata.
   */
  @Test
  public void updateChangesContractTypeOnlyWhenPassed() {
    Fixture fixture = fixture();
    assertIsOk(POST(manager(fixture), CREATE_URL, "application/json",
        "{\"personId\": " + fixture.person.id + ", \"beginDate\": \"" + BEGIN_DATE
        + "\", \"contractType\": \"interim\"}"));
    Long contractId = onlyContract(fixture.person).id;
    String updateUrl = "/rest/v2/contracts/update?id=" + contractId;

    assertIsOk(PUT(manager(fixture), updateUrl, "application/json",
        "{\"personId\": " + fixture.person.id + ", \"beginDate\": \"2026-02-01\"}"));
    Contract contract = onlyContract(fixture.person);
    assertEquals(new LocalDate(2026, 2, 1), contract.getBeginDate());
    assertEquals(ContractType.interim, contract.getContractType());
    assertFalse(contract.isOnCertificate());

    assertIsOk(PUT(manager(fixture), updateUrl, "application/json",
        "{\"personId\": " + fixture.person.id + ", \"beginDate\": \"2026-02-01\", "
        + "\"contractType\": \"structured_public_administration\"}"));
    contract = onlyContract(fixture.person);
    assertEquals(ContractType.structured_public_administration, contract.getContractType());
    assertTrue(contract.isOnCertificate());

    assertStatus(400, PUT(manager(fixture), updateUrl, "application/json",
        "{\"personId\": " + fixture.person.id + ", \"beginDate\": \"2026-02-01\", "
        + "\"contractType\": \"sconosciuto\"}"));
    assertEquals(ContractType.structured_public_administration,
        onlyContract(fixture.person).getContractType());
  }

  private static class Fixture {
    Person person;
    String managerUsername;
  }

  /**
   * Una sede con un gestore dell'anagrafica e una persona ancora senza contratti.
   */
  private Fixture fixture() {
    Office office = h2Examples.activeNormalEmployee(new LocalDate(2020, 1, 1)).getOffice();
    User manager = h2Examples.userWithOfficeRole(office, Role.REGISTRY_MANAGER);
    User employee = h2Examples.userWithOfficeRole(office, Role.EMPLOYEE);
    JPA.em().getTransaction().commit();
    JPA.em().getTransaction().begin();
    Fixture fixture = new Fixture();
    fixture.person = Person.find("user.id = ?1", employee.id).first();
    fixture.managerUsername = manager.getUsername();
    return fixture;
  }

  private Request manager(Fixture fixture) {
    Request request = newRequest();
    request.user = fixture.managerUsername;
    request.password = H2Examples.DEFAULT_PASSWORD;
    return request;
  }

  private List<Contract> contracts(Person person) {
    JPA.em().clear();
    return Contract.find("person.id = ?1", person.id).fetch();
  }

  private Contract onlyContract(Person person) {
    List<Contract> contracts = contracts(person);
    assertEquals(1, contracts.size());
    return contracts.get(0);
  }
}
