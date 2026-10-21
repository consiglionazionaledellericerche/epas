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

package dao;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import common.injection.StaticInject;
import db.h2support.H2Examples;
import java.util.List;
import models.Office;
import models.Person;
import models.PersonOffice;
import org.joda.time.LocalDate;
import org.junit.Test;
import play.db.jpa.JPA;
import play.test.UnitTest;

/**
 * Verifica che le liste di persone per sede considerino l'afferenza nel periodo richiesto: un
 * dipendente trasferito durante l'anno compare nei riepiloghi annuali di entrambe le sedi.
 */
@StaticInject
public class PersonDaoOfficeTest extends UnitTest {

  @Inject
  private static H2Examples h2Examples;

  @Inject
  private static PersonDao personDao;

  private List<Person> list(Office office, LocalDate begin, LocalDate end) {
    return personDao.list(Optional.absent(), ImmutableSet.of(office), false, begin, end, false)
        .list();
  }

  @Test
  public void transferredPersonIsListedInBothOfficesForTheYear() {
    Person person = h2Examples.activeNormalEmployee(new LocalDate(2020, 1, 1));
    final Office source = person.getOffice();

    Office destination = new Office();
    destination.setName("Sede destinazione " + person.id);
    destination.setInstitute(source.getInstitute());
    destination.setBeginDate(new LocalDate(2020, 1, 1));
    destination.setCodeId("DEST-" + person.id);
    destination.setCode("DEST-" + person.id);
    destination.save();

    person.changeOffice(destination, new LocalDate(2026, 6, 1));
    person.getPersonOffices().forEach(PersonOffice::save);
    JPA.em().flush();

    final LocalDate begin2026 = new LocalDate(2026, 1, 1);
    final LocalDate end2026 = new LocalDate(2026, 12, 31);
    assertTrue(list(source, begin2026, end2026).contains(person));
    assertTrue(list(destination, begin2026, end2026).contains(person));

    final LocalDate begin2027 = new LocalDate(2027, 1, 1);
    final LocalDate end2027 = new LocalDate(2027, 12, 31);
    assertFalse(list(source, begin2027, end2027).contains(person));
    assertTrue(list(destination, begin2027, end2027).contains(person));

    // il trasferimento decorre dal 1 giugno: a giugno solo la sede di destinazione
    assertFalse(list(source, new LocalDate(2026, 6, 1), new LocalDate(2026, 6, 30))
        .contains(person));
    assertTrue(list(source, new LocalDate(2026, 5, 1), new LocalDate(2026, 5, 31))
        .contains(person));
    assertFalse(list(destination, new LocalDate(2026, 5, 1), new LocalDate(2026, 5, 31))
        .contains(person));
  }
}
