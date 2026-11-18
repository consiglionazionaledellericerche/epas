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

package manager.sync;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import common.injection.StaticInject;
import db.h2support.H2Examples;
import manager.sync.personoffice.PersonOfficeChange;
import manager.sync.personoffice.PersonOfficeSyncManager;
import models.Office;
import models.Person;
import org.joda.time.LocalDate;
import org.junit.Test;
import play.db.jpa.JPA;
import play.test.UnitTest;

/**
 * Verifica l'applicazione delle variazioni di afferenza ricevute dai sistemi esterni.
 */
@StaticInject
public class PersonOfficeSyncManagerTest extends UnitTest {

  @Inject
  private static H2Examples h2Examples;

  @Inject
  private static PersonOfficeSyncManager personOfficeSyncManager;

  private PersonOfficeChange change(String number, String officeCodeId, LocalDate beginDate) {
    PersonOfficeChange change = new PersonOfficeChange();
    change.setNumber(number);
    change.setOfficeCodeId(officeCodeId);
    change.setBeginDate(beginDate.toString("yyyy-MM-dd"));
    return change;
  }

  @Test
  public void transferIsAppliedOnceAndIgnoredWhenRepeated() {
    final LocalDate today = LocalDate.now();
    Person person = h2Examples.activeNormalEmployee(new LocalDate(2020, 1, 1));
    final Office source = person.getOffice();
    person.setNumber("SYNC" + person.id);
    person.save();

    Office destination = new Office();
    destination.setName("Sede destinazione sync");
    destination.setInstitute(source.getInstitute());
    destination.setBeginDate(new LocalDate(2020, 1, 1));
    destination.setCodeId("SYNC-DEST-" + person.id);
    destination.setCode("SYNC-DEST-" + person.id);
    destination.save();
    JPA.em().flush();

    final PersonOfficeChange change = change(person.getNumber(), destination.getCodeId(), today);
    SyncResult result = personOfficeSyncManager.apply(Lists.newArrayList(change));
    assertTrue(result.isSuccess());

    assertEquals(destination, person.getOfficeAt(today));
    assertEquals(source, person.getOfficeAt(today.minusDays(1)));
    assertEquals(2, person.getPersonOffices().size());

    // La stessa variazione ricevuta di nuovo non produce nuove afferenze
    result = personOfficeSyncManager.apply(Lists.newArrayList(change));
    assertTrue(result.isSuccess());
    assertTrue(result.getMessages().isEmpty());
    assertEquals(2, person.getPersonOffices().size());
  }

  @Test
  public void unknownPersonIsReported() {
    SyncResult result = personOfficeSyncManager.apply(Lists.newArrayList(
        change("MATRICOLA-INESISTENTE", "000000", LocalDate.now())));
    assertTrue(result.isSuccess());
    assertEquals(1, result.getMessages().size());
  }
}
