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

package manager;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import controllers.Security;
import dao.CompetenceDao;
import dao.PersonDayDao;
import dao.PersonReperibilityDayDao;
import dao.history.HistoricalDao;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import models.Competence;
import models.CompetenceCode;
import models.Person;
import models.PersonDay;
import models.PersonReperibility;
import models.PersonReperibilityDay;
import models.PersonReperibilityType;
import models.ReperibilityTypeMonth;
import models.Role;
import models.User;
import models.dto.HolidaysReperibilityDto;
import models.dto.WorkDaysReperibilityDto;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.YearMonth;
import play.i18n.Messages;

/**
 * Gestore delle operazioni sulla reperibilità ePAS.
 *
 * @author Dario Tagliaferri
 */

@Slf4j
public class ReperibilityManager2 {

  private final PersonReperibilityDayDao reperibilityDayDao;
  private final PersonDayDao personDayDao;
  private final PersonDayManager personDayManager;
  private final CompetenceDao competenceDao;
  private final PersonReperibilityDayDao reperibilityDao;
  private final ConfigurationManager configurationManager;

  /**
   * Injection.
   *
   * @param reperibilityDayDao il dao sui giorni di reperibilità
   * @param personDayDao il dao sui personday
   * @param personDayManager il manager coi metodi sul personday
   * @param competenceDao il dao sulle competenze
   * @param reperibilityDao il dao sulla reperibilità
   */
  @Inject
  public ReperibilityManager2(PersonReperibilityDayDao reperibilityDayDao, 
      PersonDayDao personDayDao, PersonDayManager personDayManager, 
      CompetenceDao competenceDao, 
      PersonReperibilityDayDao reperibilityDao, ConfigurationManager configurationManager) {
    this.reperibilityDayDao = reperibilityDayDao;
    this.personDayDao = personDayDao;
    this.personDayManager = personDayManager;   
    this.competenceDao = competenceDao;
    this.reperibilityDao = reperibilityDao;
    this.configurationManager = configurationManager;
  }

  /**
   * La lista delle attività di reperibilità visibili all'utente che ne fa la richiesta.
   *
   * @return la lista delle attività di reperibilità visibili all'utente che ne fa la richiesta.
   */
  public List<PersonReperibilityType> getUserActivities() {
    List<PersonReperibilityType> activities = Lists.newArrayList();
    User currentUser = Security.getUser().get();
    Person person = currentUser.person;
    if (person != null) {
      if (!person.reperibilityTypes.isEmpty()) {
        activities.addAll(person.reperibilityTypes.stream()
            .sorted(Comparator.comparing(o -> o.description))
            .collect(Collectors.toList()));
      }
      if (!person.reperibilities.isEmpty()) {
        activities.addAll(person.reperibilities.stream()            
            .collect(Collectors.toList()));
      }
      if (!person.reperibility.isEmpty()) {
        for (PersonReperibility rep : person.reperibility) {
          activities.add(rep.personReperibilityType);
        }        
      }
      if (currentUser.hasRoles(Role.PERSONNEL_ADMIN)) {
        activities.addAll(currentUser.usersRolesOffices.stream()
            .flatMap(uro -> uro.office.personReperibilityTypes.stream().filter(prt -> !prt.disabled)
                .sorted(Comparator.comparing(o -> o.description)))
            .collect(Collectors.toList()));
      }
    } else {
      if (currentUser.isSystemUser()) {
        activities.addAll(PersonReperibilityType.findAll());
      }
    }
    return activities.stream().distinct().collect(Collectors.toList());
  }

  /**
   * La lista di tutte le persone abilitate su quell'attività nell'intervallo di tempo
   * specificato.
   *
   * @param reperibilityType attività di reperibilità
   * @param start data di inizio del periodo
   * @param end data di fine del periodo
   * @return La lista di tutte le persone abilitate su quell'attività nell'intervallo di tempo
   *     specificato.
   */
  public List<PersonReperibility> reperibilityWorkers(
      PersonReperibilityType reperibilityType, LocalDate start,
      LocalDate end) {
    if (reperibilityType.isPersistent() && start != null && end != null) {
      return reperibilityType.personReperibilities.stream()
          .filter(pr -> pr.dateRange().isConnected(
              Range.closed(start, end)))
          .collect(Collectors.toList());
    } else {
      return new ArrayList<>();
    }
  }

  /**
   * Salva il personReperibilityDay ed effettua i ricalcoli.
   *
   * @param personReperibilityDay il personReperibilityDay da salvare
   */
  public void save(PersonReperibilityDay personReperibilityDay) {

    Verify.verifyNotNull(personReperibilityDay).save();
    recalculate(personReperibilityDay);
  }

  /**
   * Cancella il personReperibilityDay.
   *
   * @param personReperibilityDay il personReperibilityDay da cancellare
   */
  public void delete(PersonReperibilityDay personReperibilityDay) {

    Verify.verifyNotNull(personReperibilityDay).delete();
    recalculate(personReperibilityDay);
  }


  private void recalculate(PersonReperibilityDay personReperibilityDay) {

    final PersonReperibilityType reperibilityType = personReperibilityDay.reperibilityType;

    // Aggiornamento del ReperibilityTypeMonth
    if (reperibilityType != null) {

      //FIXME: servono questi due controlli???
      // Ricalcoli sul turno
      if (personReperibilityDay.isPersistent()) {
        checkReperibilityValid(personReperibilityDay);
      }

      // Ricalcoli sui giorni coinvolti dalle modifiche
      checkReperibilityDayValid(personReperibilityDay.date, reperibilityType);

      /*
       *  Recupera la data precedente dallo storico e verifica se c'è stato un 
       *  cambio di date sul turno. In tal caso effettua il ricalcolo anche 
       *  sul giorno precedente (spostamento di un turno da un giorno all'altro)
       */
      HistoricalDao.lastRevisionsOf(PersonReperibilityDay.class, personReperibilityDay.id)
          .stream().limit(1).map(historyValue -> {
            PersonReperibilityDay pd = (PersonReperibilityDay) historyValue.value;
            return pd.date;
          }).filter(Objects::nonNull).distinct().forEach(localDate -> {
            if (!localDate.equals(personReperibilityDay.date)) {
              checkReperibilityDayValid(localDate, reperibilityType);
            }
          });

      // Aggiornamento del relativo ReperibilityTypeMonth (per incrementare il campo version)
      ReperibilityTypeMonth newStatus = 
          reperibilityType.monthStatusByDate(personReperibilityDay.date)
          .orElse(new ReperibilityTypeMonth());

      if (newStatus.personReperibilityType != null) {
        newStatus.updatedAt = LocalDateTime.now();
      } else {
        newStatus.yearMonth = new YearMonth(personReperibilityDay.date);
        newStatus.personReperibilityType = reperibilityType;
      }
      newStatus.save();

    }
  }

  /**
   * Verifica se un turno puo' essere inserito senza violare le regole dei turni.
   *
   * @param personReperibilityDay il personShiftDay da inserire
   * @return l'eventuale stringa contenente l'errore evidenziato in fase di inserimento del turno.
   */
  public Optional<String> reperibilityPermitted(PersonReperibilityDay personReperibilityDay) {

    /*
     * 0. Verificare se la persona è segnata in quell'attività in quel giorno
     *    return shift.personInactive
     * 1. La Persona non deve essere già reperibile per quel giorno
     * 2. La Persona non deve avere assenze giornaliere.
     * 3. La reperibilità non sia già presente  
     * 4. Controllare anche il quantitativo di giorni di reperibilità feriale e festiva massimi?  
     */

    //Verifica se la persona è attiva in quell'attività in quel giorno
    Optional<PersonReperibility> rep = reperibilityDao
        .byPersonDateAndType(personReperibilityDay.personReperibility.person, 
            personReperibilityDay.date, personReperibilityDay.reperibilityType);
    if (!rep.isPresent()) {
      return Optional.of(Messages.get("reperibility.personInactive"));
    }

    // Verifica che la persona non abbia altre reperibilità nello stesso giorno 
    final Optional<PersonReperibilityDay> personReperibility = reperibilityDayDao
        .getPersonReperibilityDay(
            personReperibilityDay.personReperibility.person, personReperibilityDay.date);

    if (personReperibility.isPresent()) {
      return Optional.of(Messages.get("reperibility.alreadyInReperibility", 
          personReperibility.get().reperibilityType));
    }

    // verifica che la persona non sia assente nel giorno
    final Optional<PersonDay> personDay = personDayDao
        .getPersonDay(personReperibilityDay.personReperibility.person, personReperibilityDay.date);

    if (personDay.isPresent() 
        && !personDayManager.isAbsenceCompatibleWithReperibility(personDay.get())) {
      return Optional.of(Messages.get("reperibility.absenceInDay"));
    }

    List<PersonReperibilityDay> list = reperibilityDayDao
        .getPersonReperibilityDayFromPeriodAndType(
            personReperibilityDay.date, personReperibilityDay.date,
            personReperibilityDay.reperibilityType, Optional.absent());

    //controlla che la reperibilità nel giorno sia già stata assegnata ad un'altra persona
    if (!list.isEmpty()) {
      return Optional.of(Messages
          .get("reperibility.dayAlreadyAssigned", 
              personReperibilityDay.personReperibility.person.fullName()));
    }

    return Optional.absent();
  }


  public void checkReperibilityValid(PersonReperibilityDay personReperibilityDay) {
    /*
     * 0. Dev'essere una reperibilità persistente.
     * 1. Non ci siano assenze giornaliere
     * 2. Non ci devono essere già reperibili per quel giorno
     * 3. 
     */
    //TODO: va implementato davvero?
  }

  public void checkReperibilityDayValid(LocalDate date, PersonReperibilityType type) {
    //TODO: va implementato davvero?
  }

  /**
   * Ritorna una mappa con i giorni maturati di reperibilità per persona.
   *
   * @param reperibility attività sulla quale effettuare i calcoli
   * @param from data di inizio da cui calcolare
   * @param to data di fine
   * @return Restituisce una mappa con i giorni di reperibilità maturati per ogni persona.
   */
  public Map<Person, Integer> calculateReperibilityWorkDaysCompetences(
      PersonReperibilityType reperibility, LocalDate from, LocalDate to) {

    final Map<Person, Integer> reperibilityWorkDaysCompetences = new HashMap<>();

    final LocalDate today = LocalDate.now();

    final LocalDate lastDay;

    if ((Boolean) configurationManager.configValue(reperibility.office, 
        EpasParam.ENABLE_REPERIBILITY_APPROVAL_BEFORE_END_MONTH)) {
      lastDay = to;
    } else {
      if (to.isAfter(today)) {
        lastDay = today;
      } else {
        lastDay = to;
      }
    }
    CompetenceCode code = reperibility.monthlyCompetenceType.workdaysCode;        
    involvedReperibilityWorkers(reperibility, from, to).forEach(person -> {
      int competences = 
          calculatePersonReperibilityCompetencesInPeriod(reperibility, person, from, lastDay, code);
      reperibilityWorkDaysCompetences.put(person, competences);
    });

    return reperibilityWorkDaysCompetences;
  }

  /**
   * Una lista di persone che sono effettivamente coinvolte in reperibilità in un 
   * determinato periodo (Dipendenti con le reperibilità attive in quel periodo).
   *
   * @param reperibility attività di reperibilità
   * @param from data di inizio
   * @param to data di fine
   * @return Una lista di persone che sono effettivamente coinvolte in reperibilità in un 
   *     determinato periodo (Dipendenti con le reperibilità attive in quel periodo).
   */
  public List<Person> involvedReperibilityWorkers(PersonReperibilityType reperibility, 
      LocalDate from, LocalDate to) {
    return reperibilityDayDao.byTypeAndPeriod(reperibility, from, to)
        .stream().map(rep -> rep.person).distinct().collect(Collectors.toList());
  }

  /**
   * Il numero di giorni di competenza maturati in base alle reperibilità effettuate
   * nel periodo selezionato (di norma serve calcolarli su un intero mese al massimo).
   *
   * @param reperibility attività di turno
   * @param person Persona sulla quale effettuare i calcoli
   * @param from data iniziale
   * @param to data finale
   * @return il numero di giorni di competenza maturati in base alle reperibilità effettuate
   *     nel periodo selezionato (di norma serve calcolarli su un intero mese al massimo).
   */
  public int calculatePersonReperibilityCompetencesInPeriod(
      PersonReperibilityType reperibility, Person person, 
      LocalDate from, LocalDate to, CompetenceCode code) {

    // TODO: 08/06/17 Sicuramente vanno differenziati per tipo di competenza.....
    // c'è sono da capire qual'è la discriminante
    int reperibilityCompetences = 0;
    final List<PersonReperibilityDay> reperibilities = reperibilityDayDao
        .getPersonReperibilityDaysByPeriodAndType(from, to, reperibility, person);

    if (code.equals(reperibility.monthlyCompetenceType.workdaysCode)) {
      reperibilityCompetences = (int) reperibilities.stream()
          .filter(rep -> !personDayManager.isHoliday(person, rep.date)).count();
    } else {
      reperibilityCompetences = (int) reperibilities.stream()
          .filter(rep -> personDayManager.isHoliday(person, rep.date)).count();
    }

    return reperibilityCompetences;
  }

  /**
   * La mappa contenente i giorni di reperibilità festiva per ogni dipendente reperibile.
   *
   * @param reperibility il tipo di reperibilità 
   * @param start la data di inizio da cui conteggiare
   * @param end la data di fine entro cui conteggiare
   * @return la mappa contenente i giorni di reperibilità festiva per ogni dipendente reperibile.
   */
  public Map<Person, Integer> calculateReperibilityHolidaysCompetences(
      PersonReperibilityType reperibility, LocalDate start, LocalDate end) {

    final Map<Person, Integer> reperibilityHolidaysCompetences = new HashMap<>();

    final LocalDate today = LocalDate.now();

    final LocalDate lastDay;

    if ((Boolean) configurationManager.configValue(reperibility.office, 
        EpasParam.ENABLE_REPERIBILITY_APPROVAL_BEFORE_END_MONTH)) {
      lastDay = end;
    } else {
      if (end.isAfter(today)) {
        lastDay = today;
      } else {
        lastDay = end;
      }
    }
    CompetenceCode code = reperibility.monthlyCompetenceType.holidaysCode;        
    involvedReperibilityWorkers(reperibility, start, end).forEach(person -> {
      int competences = calculatePersonReperibilityCompetencesInPeriod(reperibility, 
          person, start, lastDay, code);
      reperibilityHolidaysCompetences.put(person, competences);
    });

    return reperibilityHolidaysCompetences;
  }




  /**
   * Effettua i calcoli delle competenze relative alle reperibilità sulle attività approvate 
   * per le persone coinvolte in una certa attività e un determinato mese. 
   * Da utilizzare in seguito ad ogni approvazione/disapprovazione delle reperibilità.
   *
   * @param reperibilityTypeMonth lo stato dell'attività di reperibilità in un determinato mese
   */
  public void assignReperibilityCompetences(ReperibilityTypeMonth reperibilityTypeMonth) {
    Verify.verifyNotNull(reperibilityTypeMonth);
    //stabilisco le date di inizio e fine periodo da considerare per i calcoli
    final LocalDate monthBegin = reperibilityTypeMonth.yearMonth.toLocalDate(1);
    final LocalDate monthEnd = monthBegin.dayOfMonth().withMaximumValue();
    final int year = reperibilityTypeMonth.yearMonth.getYear();
    final int month = reperibilityTypeMonth.yearMonth.getMonthOfYear();

    final LocalDate today = LocalDate.now();

    final LocalDate lastDay;
    
    if ((Boolean) configurationManager.configValue(reperibilityTypeMonth
        .personReperibilityType.office, 
        EpasParam.ENABLE_REPERIBILITY_APPROVAL_BEFORE_END_MONTH)) {
      lastDay = monthEnd;
    } else {
      if (monthEnd.isAfter(today)) {
        lastDay = today;
      } else {
        lastDay = monthEnd;
      }
    }
    
    //cerco le persone reperibili nel periodo di interesse
    final List<Person> involvedReperibilityPeople = involvedReperibilityWorkers(
        reperibilityTypeMonth.personReperibilityType, monthBegin, monthEnd);
    CompetenceCode reperibilityHoliday = reperibilityTypeMonth.personReperibilityType
        .monthlyCompetenceType.holidaysCode;        
    CompetenceCode reperibilityWorkdays = reperibilityTypeMonth.personReperibilityType
        .monthlyCompetenceType.workdaysCode;

    //per ogni persona approvo le reperibilità feriali e festive 
    involvedReperibilityPeople.forEach(person ->  {
      WorkDaysReperibilityDto dto = new WorkDaysReperibilityDto();
      dto.person = person;
      dto.workdaysReperibility = calculatePersonReperibilityCompetencesInPeriod(
          reperibilityTypeMonth.personReperibilityType, person, 
          monthBegin, lastDay, reperibilityWorkdays);
      dto.workdaysPeriods = getReperibilityPeriod(person, monthBegin, monthEnd, 
          reperibilityTypeMonth.personReperibilityType, false);

      HolidaysReperibilityDto dto2 = new HolidaysReperibilityDto();
      dto2.person = person;
      dto2.holidaysReperibility = calculatePersonReperibilityCompetencesInPeriod(
          reperibilityTypeMonth.personReperibilityType, person, 
          monthBegin, lastDay, reperibilityHoliday);
      dto2.holidaysPeriods = getReperibilityPeriod(person, monthBegin, monthEnd, 
          reperibilityTypeMonth.personReperibilityType, true);

      Optional<Competence> reperibilityHolidayCompetence = competenceDao
          .getCompetence(person, year, month, reperibilityHoliday);

      Competence holidayCompetence = reperibilityHolidayCompetence
          .or(new Competence(person, reperibilityHoliday, year, month));
      holidayCompetence.valueApproved = dto2.holidaysReperibility;
      holidayCompetence.reason = getReperibilityDates(dto2.holidaysPeriods);
      holidayCompetence.save();

      log.info("Salvata {}", holidayCompetence);

      Optional<Competence> reperibilityWorkdaysCompetence = competenceDao
          .getCompetence(person, year, month, reperibilityWorkdays);

      Competence workdayCompetence = reperibilityWorkdaysCompetence
          .or(new Competence(person, reperibilityWorkdays, year, month));
      workdayCompetence.valueApproved = dto.workdaysReperibility;
      workdayCompetence.reason = getReperibilityDates(dto.workdaysPeriods);
      workdayCompetence.save();

      log.info("Salvata {}", workdayCompetence);
    });

  }

  /**
   * La lista dei range di date in cui un dipendente è stato reperibile.
   *
   * @param person il reperibile
   * @param begin la data da cui cercare i giorni di reperibilità
   * @param end la data entro cui cercare i giorni di reperibilità
   * @param type l'attività su cui è reperibile il dipendente
   * @param holidays true se occcorre filtrare sui giorni false se occorre filtrare sui feriali
   * @return la lista dei range di date in cui un dipendente è stato reperibile.
   */
  public List<Range<LocalDate>> getReperibilityPeriod(Person person, LocalDate begin, 
      LocalDate end, PersonReperibilityType type, boolean holidays) {

    List<PersonReperibilityDay> days = reperibilityDao
        .getPersonReperibilityDaysByPeriodAndType(begin, end, type, person);

    List<PersonReperibilityDay> newList = null;
    if (holidays) {
      newList = days.stream().filter(
          day -> personDayManager.isHoliday(person, day.date)).collect(Collectors.toList());
    } else {
      newList = days.stream().filter(
          day -> !personDayManager.isHoliday(person, day.date)).collect(Collectors.toList());
    }
    if (newList.isEmpty()) {
      return null;
    }
    LocalDate first = newList.get(0).date;
    List<Range<LocalDate>> list = Lists.newArrayList();
    Range<LocalDate> range = null;

    for (PersonReperibilityDay day : newList) {
      if (first.equals(day.date)) {
        range = Range.closed(day.date, day.date);
      } else {
        if (day.date.equals(range.upperEndpoint().plusDays(1))) {
          range = Range.closed(range.lowerEndpoint(), day.date);
        } else {
          list.add(range);
          range = Range.closed(day.date, day.date);
        }
      }
    }
    list.add(range);
    return list;
  }

  /**
   * La stringa formattata contenente le date dei giorni di reperibilità effettuati.
   *
   * @param list la lista dei periodi di reperibilità all'interno del mese
   * @return la stringa formattata contenente le date dei giorni di reperibilità effettuati.
   */
  private String getReperibilityDates(List<Range<LocalDate>> list) {
    String str = "";
    if (list == null || list.isEmpty()) {
      return str;
    }
    for (Range<LocalDate> range : list) {
      str = str + range.lowerEndpoint().getDayOfMonth() 
          + "-" + range.upperEndpoint().getDayOfMonth() + "/" 
          + range.lowerEndpoint().monthOfYear().getAsText() + " ";
    }
    return str;
  }

}