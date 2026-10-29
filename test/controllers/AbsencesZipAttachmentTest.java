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
import dao.absences.AbsenceComponentDao;
import db.h2support.H2Examples;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipInputStream;
import javax.inject.Inject;
import manager.services.absences.AbsenceService;
import models.Office;
import models.Person;
import models.PersonDay;
import models.Role;
import models.absences.Absence;
import models.absences.JustifiedType.JustifiedTypeName;
import org.joda.time.LocalDate;
import org.junit.Test;
import play.db.jpa.Blob;
import play.db.jpa.JPA;
import play.mvc.Http.Response;
import play.mvc.Router;
import play.test.FunctionalTest;

/**
 * Test funzionali su Absences.zipAttachment: lo zip contiene solo gli allegati delle persone
 * della sede indicata e solo chi ha i permessi su quella sede può scaricarlo.
 */
@StaticInject
public class AbsencesZipAttachmentTest extends FunctionalTest {

  private static final LocalDate BEGIN_2026 = new LocalDate(2026, 1, 1);
  private static final LocalDate ABSENCE_DATE = new LocalDate(2026, 3, 10);
  private static final String CODE = "92";

  @Inject
  private static H2Examples h2Examples;
  @Inject
  private static AbsenceService absenceService;
  @Inject
  private static AbsenceComponentDao absenceComponentDao;

  /**
   * L'amministratore del personale non può scaricare gli allegati di un'altra sede.
   */
  @Test
  public void personnelAdminCannotZipAttachmentsOfAnotherOffice() {
    absenceService.enumInitializator();
    Person colleague = h2Examples.activeNormalEmployee(BEGIN_2026);
    String admin = adminOf(colleague.getOffice());
    Person other = h2Examples.activeNormalEmployee(BEGIN_2026);
    absenceWithAttachment(other);
    commit();

    login(admin);
    assertStatus(403, GET(zipUrl(other.getOffice())));
  }

  /**
   * Lo zip della propria sede non contiene gli allegati delle persone di altre sedi.
   */
  @Test
  public void zipContainsOnlyAttachmentsOfTheOffice() throws IOException {
    absenceService.enumInitializator();
    Person colleague = h2Examples.activeNormalEmployee(BEGIN_2026);
    absenceWithAttachment(colleague);
    String admin = adminOf(colleague.getOffice());
    Person other = h2Examples.activeNormalEmployee(BEGIN_2026);
    absenceWithAttachment(other);
    commit();

    login(admin);
    Response response = GET(zipUrl(colleague.getOffice()));
    assertIsOk(response);
    assertEquals(1, zipEntries(response));
  }

  private String adminOf(Office office) {
    return h2Examples.userWithOfficeRole(office, Role.PERSONNEL_ADMIN).getUsername();
  }

  private void absenceWithAttachment(Person person) {
    PersonDay personDay = new PersonDay(person, ABSENCE_DATE);
    personDay.save();
    Absence absence = new Absence();
    absence.setPersonDay(personDay);
    absence.setAbsenceType(absenceComponentDao.absenceTypeByCode(CODE).get());
    absence.setJustifiedType(absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.complete_day_and_add_overtime));
    Blob file = new Blob();
    file.set(new ByteArrayInputStream(
        ("allegato di " + person.getFullname()).getBytes(StandardCharsets.UTF_8)), "text/plain");
    absence.setAbsenceFile(file);
    absence.save();
  }

  private String zipUrl(Office office) {
    Map<String, Object> args = new HashMap<>();
    args.put("code", CODE);
    args.put("year", ABSENCE_DATE.getYear());
    args.put("month", ABSENCE_DATE.getMonthOfYear());
    args.put("officeId", office.id);
    return Router.reverse("Absences.zipAttachment", args).url;
  }

  private int zipEntries(Response response) throws IOException {
    int entries = 0;
    try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(
        response.out.toByteArray()))) {
      while (zip.getNextEntry() != null) {
        entries++;
      }
    }
    return entries;
  }

  private void commit() {
    JPA.em().getTransaction().commit();
    JPA.em().getTransaction().begin();
  }

  private void login(String username) {
    POST("/login?username=" + username + "&password=" + H2Examples.DEFAULT_PASSWORD);
  }
}
