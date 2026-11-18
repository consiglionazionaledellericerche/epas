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

package helpers;

import java.io.ByteArrayInputStream;
import models.Person;
import models.PersonDay;
import models.absences.Absence;
import models.absences.AbsenceType;
import org.joda.time.LocalDate;
import org.junit.Test;
import play.db.jpa.Blob;
import play.test.UnitTest;

/**
 * Nome dei file degli allegati delle assenze: anno-mese-giorno-codice-cognome-nome (#703).
 */
public class AttachmentFileNamesTest extends UnitTest {

  private Absence absence(String code, String surname, String name, String fileType) {
    Person person = new Person();
    person.setSurname(surname);
    person.setName(name);
    AbsenceType absenceType = new AbsenceType();
    absenceType.setCode(code);
    Absence absence = new Absence();
    absence.setPersonDay(new PersonDay(person, new LocalDate(2026, 11, 8)));
    absence.setAbsenceType(absenceType);
    if (fileType != null) {
      Blob blob = new Blob();
      blob.set(new ByteArrayInputStream(new byte[] {1}), fileType);
      absence.setAbsenceFile(blob);
    }
    return absence;
  }

  @Test
  public void nameWithYearMonthDayCodeAndPerson() {
    assertEquals("2026-11-08-111-Rossi-Mario.pdf",
        AttachmentFileNames.forAbsence(absence("111", "Rossi", "Mario", "application/pdf")));
  }

  @Test
  public void unsafeCharactersAreReplaced() {
    assertEquals("2026-11-08-18M-D-Angelo-Nicolo-Maria.jpg",
        AttachmentFileNames.forAbsence(
            absence("18M", "D'Angelo", " Nicolò Maria ", "image/jpeg")));
  }

  @Test
  public void unknownTypeHasNoExtension() {
    assertEquals("2026-11-08-31-Bianchi-Anna",
        AttachmentFileNames.forAbsence(absence("31", "Bianchi", "Anna", null)));
  }
}
