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

package manager.sync.personoffice;

import com.google.common.base.Strings;
import dao.OfficeDao;
import dao.PersonDao;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.PersonOfficeManager;
import manager.sync.SyncResult;
import models.Office;
import models.Person;
import models.PersonOffice;
import org.joda.time.LocalDate;

/**
 * Applica in ePAS le variazioni di afferenza ricevute da un sistema esterno.
 */
@Slf4j
public class PersonOfficeSyncManager {

  private final PersonOfficeChangesClient client;
  private final PersonOfficeManager personOfficeManager;
  private final PersonDao personDao;
  private final OfficeDao officeDao;

  /**
   * Default constructor.
   */
  @Inject
  public PersonOfficeSyncManager(PersonOfficeChangesClient client,
      PersonOfficeManager personOfficeManager, PersonDao personDao, OfficeDao officeDao) {
    this.client = client;
    this.personOfficeManager = personOfficeManager;
    this.personDao = personDao;
    this.officeDao = officeDao;
  }

  /**
   * True se la sincronizzazione è abilitata.
   */
  public boolean isEnabled() {
    return client.isEnabled();
  }

  /**
   * Scarica e applica le variazioni registrate dal sistema esterno a partire da since.
   */
  public SyncResult sync(LocalDate since) {
    return apply(client.changesSince(since));
  }

  /**
   * Applica una lista di variazioni di afferenza. Le variazioni già presenti in ePAS vengono
   * ignorate, quindi la stessa variazione può essere ricevuta più volte.
   */
  public SyncResult apply(List<PersonOfficeChange> changes) {
    final SyncResult result = new SyncResult();
    for (PersonOfficeChange change : changes) {
      try {
        applyChange(change, result);
      } catch (RuntimeException ex) {
        log.error("Errore nell'applicazione della variazione di afferenza {}", change, ex);
        result.setFailed().add(String.format("Errore per %s: %s", change, ex.getMessage()));
      }
    }
    return result;
  }

  private void applyChange(PersonOfficeChange change, SyncResult result) {
    final Person person = findPerson(change);
    if (person == null) {
      result.add(String.format("Persona non trovata in ePAS: matricola %s, codice fiscale %s",
          change.getNumber(), change.getFiscalCode()));
      return;
    }
    final Office office = Strings.isNullOrEmpty(change.getOfficeCodeId())
        ? null : officeDao.byCodeId(change.getOfficeCodeId()).orNull();
    if (office == null) {
      result.add(String.format("Sede con codice %s non trovata per %s",
          change.getOfficeCodeId(), person.getFullname()));
      return;
    }
    final LocalDate beginDate = LocalDate.parse(change.getBeginDate());
    final LocalDate endDate = Strings.isNullOrEmpty(change.getEndDate())
        ? null : LocalDate.parse(change.getEndDate());

    final boolean alreadyPresent = person.getPersonOffices().stream()
        .anyMatch(po -> po.getOffice().equals(office) && po.getBeginDate().equals(beginDate));
    if (!alreadyPresent) {
      personOfficeManager.changeOffice(person, office, beginDate).getMessages()
          .forEach(m -> result.add(person.getFullname() + ": " + m));
      result.add(String.format("%s afferisce a %s dal %s", person.getFullname(),
          office.getName(), beginDate.toString("dd/MM/yyyy")));
    }
    if (endDate != null) {
      final PersonOffice personOffice = person.getPersonOffices().stream()
          .filter(po -> po.getOffice().equals(office) && po.getBeginDate().equals(beginDate))
          .findFirst().get();
      if (!endDate.equals(personOffice.getEndDate())) {
        final List<String> errors =
            personOfficeManager.update(personOffice, office, beginDate, endDate);
        result.add(errors.isEmpty()
            ? String.format("Chiusa al %s l'afferenza di %s a %s",
                endDate.toString("dd/MM/yyyy"), person.getFullname(), office.getName())
            : String.join(". ", errors));
      }
    }
  }

  private Person findPerson(PersonOfficeChange change) {
    Person person = null;
    if (!Strings.isNullOrEmpty(change.getNumber())) {
      person = personDao.getPersonByNumber(change.getNumber());
    }
    if (person == null && !Strings.isNullOrEmpty(change.getFiscalCode())) {
      person = personDao.byFiscalCode(change.getFiscalCode()).orNull();
    }
    return person;
  }
}
