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

package manager;

import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import dao.PersonOfficeDao;
import dao.RoleDao;
import dao.UsersRolesOfficesDao;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import models.Office;
import models.Person;
import models.PersonOffice;
import models.Role;
import models.UsersRolesOffices;
import org.joda.time.LocalDate;

/**
 * Gestione delle afferenze periodicizzate delle persone alle sedi.
 * Tutti i cambi di sede (interfaccia di amministrazione, trasferimento massivo, sincronizzazione
 * con sistemi esterni) devono passare da questo manager.
 */
@Slf4j
public class PersonOfficeManager {

  private final UsersRolesOfficesDao uroDao;
  private final RoleDao roleDao;
  private final OfficeManager officeManager;
  private final RegistryNotificationManager registryNotificationManager;
  private final PersonOfficeDao personOfficeDao;

  /**
   * Default constructor.
   */
  @Inject
  public PersonOfficeManager(UsersRolesOfficesDao uroDao, RoleDao roleDao,
      OfficeManager officeManager, RegistryNotificationManager registryNotificationManager,
      PersonOfficeDao personOfficeDao) {
    this.uroDao = uroDao;
    this.roleDao = roleDao;
    this.officeManager = officeManager;
    this.registryNotificationManager = registryNotificationManager;
    this.personOfficeDao = personOfficeDao;
  }

  /**
   * Trasferisce la persona nella sede indicata a partire dalla data specificata, chiudendo
   * l'afferenza in corso il giorno precedente. Se il trasferimento è già efficace allinea il
   * ruolo di dipendente alla nuova sede e notifica il cambio di sede.
   *
   * @param person la persona da trasferire
   * @param office la sede di destinazione
   * @param date la data di decorrenza
   * @return l'afferenza verso la sede di destinazione.
   */
  public PersonOffice changeOffice(Person person, Office office, LocalDate date) {
    Verify.verifyNotNull(person);
    Verify.verifyNotNull(office);
    Verify.verifyNotNull(date);

    final Office oldOffice = person.getOffice();
    final PersonOffice personOffice = person.changeOffice(office, date);
    person.getPersonOffices().forEach(PersonOffice::save);
    log.info("Afferenza di {} alla sede {} dal {}", person.getFullname(), office.getName(), date);

    if (!date.isAfter(LocalDate.now())) {
      alignEmployeeRole(person);
      if (oldOffice != null && !oldOffice.equals(person.getOffice())) {
        registryNotificationManager.notifyPersonHasChangedOffice(person, oldOffice);
      }
    }
    return personOffice;
  }

  /**
   * Modifica sede e date di un'afferenza esistente.
   *
   * @return la lista degli errori di validazione, vuota se la modifica è stata salvata.
   */
  public List<String> update(PersonOffice personOffice, Office office, LocalDate beginDate,
      LocalDate endDate) {
    final Person person = personOffice.getPerson();
    final Office oldOffice = person.getOffice();
    personOffice.setOffice(office);
    personOffice.setBeginDate(beginDate);
    personOffice.setEndDate(endDate);
    final List<String> errors = validate(person);
    if (!errors.isEmpty()) {
      personOffice.refresh();
      return errors;
    }
    personOffice.save();
    alignEmployeeRole(person);
    if (oldOffice != null && !oldOffice.equals(person.getOffice())) {
      registryNotificationManager.notifyPersonHasChangedOffice(person, oldOffice);
    }
    return errors;
  }

  /**
   * Elimina un'afferenza. Non è possibile eliminare l'unica afferenza della persona.
   *
   * @return la lista degli errori, vuota se l'afferenza è stata eliminata.
   */
  public List<String> delete(PersonOffice personOffice) {
    final Person person = personOffice.getPerson();
    if (person.getPersonOffices().size() <= 1) {
      return Lists.newArrayList("Non è possibile eliminare l'unica afferenza della persona");
    }
    person.getPersonOffices().remove(personOffice);
    personOffice.delete();
    alignEmployeeRole(person);
    return Lists.newArrayList();
  }

  /**
   * Verifica che le afferenze della persona abbiano date coerenti e non si sovrappongano.
   *
   * @return la lista degli errori riscontrati.
   */
  public List<String> validate(Person person) {
    final List<String> errors = Lists.newArrayList();
    final List<PersonOffice> sorted = person.getPersonOffices().stream()
        .sorted(Comparator.comparing(PersonOffice::getBeginDate))
        .collect(Collectors.toList());
    PersonOffice previous = null;
    for (PersonOffice po : sorted) {
      if (po.getEndDate() != null && po.getEndDate().isBefore(po.getBeginDate())) {
        errors.add(String.format("L'afferenza a %s termina prima di iniziare",
            po.getOffice().getName()));
      }
      if (previous != null
          && (previous.getEndDate() == null || !previous.getEndDate().isBefore(po.getBeginDate()))) {
        errors.add(String.format("L'afferenza a %s si sovrappone a quella a %s",
            po.getOffice().getName(), previous.getOffice().getName()));
      }
      previous = po;
    }
    return errors;
  }

  /**
   * Allinea il ruolo di dipendente della persona alla sede di afferenza corrente.
   */
  public void alignEmployeeRole(Person person) {
    final Office current = person.getOffice();
    if (current == null || person.getUser() == null) {
      return;
    }
    final List<UsersRolesOffices> uroList = uroDao.getUsersRolesOfficesByUser(person.getUser());
    for (UsersRolesOffices uro : uroList) {
      if (uro.getRole().getName().equals(Role.EMPLOYEE) && !uro.getOffice().equals(current)) {
        uro.delete();
      }
    }
    officeManager.setUro(person.getUser(), current, roleDao.getRoleByName(Role.EMPLOYEE));
  }

  /**
   * Rende efficaci i trasferimenti che decorrono dalla data indicata (inseriti con data futura):
   * allinea il ruolo di dipendente e notifica il cambio di sede.
   *
   * @return il numero di trasferimenti applicati.
   */
  public int applyTransfersStartingAt(LocalDate date) {
    final List<PersonOffice> starting = personOfficeDao.startingAt(date);
    for (PersonOffice po : starting) {
      final Person person = po.getPerson();
      alignEmployeeRole(person);
      final Office oldOffice = person.getOfficeAt(date.minusDays(1));
      if (oldOffice != null && !oldOffice.equals(po.getOffice())) {
        registryNotificationManager.notifyPersonHasChangedOffice(person, oldOffice);
      }
    }
    return starting.size();
  }
}
