/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
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

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.Sets;
import common.security.SecurityRules;
import dao.AbsenceDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.TimeVariationDao;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import manager.ConsistencyManager;
import manager.PersonManager;
import manager.TimeVariationManager;
import models.Office;
import models.Person;
import models.TimeVariation;
import models.User;
import models.absences.Absence;
import models.absences.JustifiedType.JustifiedTypeName;
import models.dto.AbsenceToRecoverDto;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Variazione di tempo applicate in funzione di specifiche
 * assenze (per esempio per recupero 91CE).
 *
 * @author Cristian Lucchesi
 *
 */
@With({Resecure.class})
public class TimeVariations extends Controller {

  @Inject
  private static AbsenceDao absenceDao;
  @Inject
  static SecurityRules rules;
  @Inject
  private static TimeVariationManager timeVariationManager;
  @Inject
  private static ConsistencyManager consistencyManager;
  @Inject
  private static TimeVariationDao timeVariationDao;
  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static PersonManager personManager;
  @Inject
  private static PersonDao personDao;

  /**
   * Action che abilita la finestra di assegnamento di una variazione.
   *
   * @param absenceId l'id dell'assenza da compensare
   */
  public static void addVariation(long absenceId) {
    final Absence absence = absenceDao.getAbsenceById(absenceId);
    notFoundIfNull(absence);
    rules.checkIfPermitted(absence.personDay.person);
    int totalTimeRecovered = absence.timeVariations.stream().mapToInt(i -> i.timeVariation).sum();
    int difference = absence.timeToRecover - totalTimeRecovered;
    int hours = difference / 60;
    int minutes = difference % 60;
    LocalDate dateVariation = LocalDate.now();
    render(absence, hours, minutes, dateVariation);
  }

  /**
   * Metodo che permette il salvataggio della variazione oraria da associare ai 91CE presi in 
   * precedenza.
   *
   * @param absenceId l'id dell'assenza da giustificare
   * @param hours le ore da restituire
   * @param minutes i minuti da restituire
   */
  public static void saveVariation(long absenceId, int hours, int minutes, 
      Optional<LocalDate> dateVariation) {
    final Absence absence = absenceDao.getAbsenceById(absenceId);
    notFoundIfNull(absence);
    rules.checkIfPermitted(absence.personDay.person);
    if (absence.personDay.date.isAfter(LocalDate.now())) {
      flash.error("Non si può recuperare un'assenza %s - %s prima che questa sia sopraggiunta", 
          absence.absenceType.code, absence.absenceType.description);
      Stampings.personStamping(absence.personDay.person.id, 
          LocalDate.now().getYear(), LocalDate.now().getMonthOfYear());
    }
    TimeVariation timeVariation = timeVariationManager
        .create(absence, hours, minutes, dateVariation);
    
    timeVariation.save();
    consistencyManager
    .updatePersonSituation(absence.personDay.person.id, dateVariation.or(LocalDate.now()));
    flash.success("Aggiornato recupero ore per assenza %s in data %s", 
        absence.absenceType.code, absence.personDay.date);
    Stampings.personStamping(absence.personDay.person.id, 
        LocalDate.now().getYear(), LocalDate.now().getMonthOfYear());
  }

  /**
   * Metodo che rimuove la variazione oraria corrispondente all'id passato come parametro.
   *
   * @param timeVariationId l'id della variazione oraria
   */
  public static void removeVariation(long timeVariationId) {
    final TimeVariation timeVariation = timeVariationDao.getById(timeVariationId);
    notFoundIfNull(timeVariation);
    rules.checkIfPermitted(timeVariation.absence.personDay.person);

    Absence absence = timeVariation.absence;
    timeVariation.delete();
    consistencyManager
    .updatePersonSituation(absence.personDay.person.id, timeVariation.dateVariation);
    flash.success("Rimossa variazione oraria per il %s del giorno %s", 
        absence.absenceType.code, absence.personDay.date);
    Stampings.personStamping(absence.personDay.person.id, 
        LocalDate.now().getYear(), LocalDate.now().getMonthOfYear());

  }
  
  /**
   * Metodo che genera il template per la visualizzazione della situazione dei 91CE dei
   * dipendenti di una sede.
   *
   * @param officeId l'id della sede di cui visualizzare i dipendenti
   */
  public static void show(long officeId) {
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);
    Set<Office> offices = Sets.newHashSet();
    offices.add(office);
    List<Person> personList = personDao.getActivePersonInMonth(offices, 
        new YearMonth(LocalDate.now().getYear(), LocalDate.now().getMonthOfYear()));
    Map<Person, List<AbsenceToRecoverDto>> map = timeVariationManager.createMap(personList, office);
    render(map, office);
  }
  
  /**
   * Metodo che ritorna la situazione di una persona.
   */
  public static void personShow() {
    
    Optional<User> user = Security.getUser();
    Verify.verify(user.isPresent());
    Verify.verifyNotNull(user.get().person);

    Person person = user.get().person;
    
    LocalDate date = person.office.beginDate;
    List<Absence> absenceList = absenceDao.getAbsenceByCodeInPeriod(
        Optional.fromNullable(person), Optional.absent(), date, LocalDate.now(), 
        Optional.fromNullable(JustifiedTypeName.recover_time), false, true);
    List<AbsenceToRecoverDto> dtoList = personManager.dtoList(absenceList);
     
    render(dtoList, person);
    
  }
}
