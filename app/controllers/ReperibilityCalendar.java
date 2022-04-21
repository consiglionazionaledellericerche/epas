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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import common.security.SecurityRules;
import dao.AbsenceDao;
import dao.CompetenceCodeDao;
import dao.PersonDao;
import dao.PersonReperibilityDayDao;
import dao.ReperibilityTypeMonthDao;
import dao.RoleDao;
import dao.UsersRolesOfficesDao;
import helpers.TemplateExtensions;
import helpers.Web;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.PersonDayManager;
import manager.ReperibilityManager2;
import models.CompetenceCode;
import models.Person;
import models.PersonReperibility;
import models.PersonReperibilityDay;
import models.PersonReperibilityType;
import models.PersonShiftDay;
import models.ReperibilityTypeMonth;
import models.absences.Absence;
import models.absences.JustifiedType.JustifiedTypeName;
import models.dto.HolidaysReperibilityDto;
import models.dto.PNotifyObject;
import models.dto.ReperibilityEvent;
import models.dto.WorkDaysReperibilityDto;
import models.enumerate.EventColor;
import org.assertj.core.util.Lists;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Router;
import play.mvc.With;

/**
 * Controller per la generazione e gestione dei calendari di reperibilità.
 *
 * @author dario
 *
 */
@With(Resecure.class)
@Slf4j
public class ReperibilityCalendar extends Controller {

  @Inject
  static SecurityRules rules;
  @Inject
  static ReperibilityManager2 reperibilityManager2;
  @Inject
  static PersonReperibilityDayDao reperibilityDao;
  @Inject
  static AbsenceDao absenceDao;
  @Inject
  static ObjectMapper mapper;
  @Inject
  static ReperibilityTypeMonthDao reperibilityTypeMonthDao;
  @Inject
  static PersonDao personDao;
  @Inject
  static CompetenceCodeDao competenceCodeDao;
  @Inject
  static PersonDayManager personDayManager;
  @Inject
  static UsersRolesOfficesDao uroDao;
  @Inject
  static RoleDao roleDao;

  /**
   * ritorna alla view le info necessarie per creare il calendario.
   *
   * @param reperibility l'attività
   * @param date la data
   */
  public static void show(PersonReperibilityType reperibility, LocalDate date) {

    final LocalDate currentDate = Optional.fromNullable(date).or(LocalDate.now());

    final List<PersonReperibilityType> reperibilities = reperibilityManager2.getUserActivities();

    if (reperibilities.isEmpty()) {
      log.debug("Richiesta visualizzazione reperibilità ma nessun servizio di "
          + "reperibilità presente");
      flash.error("Nessun tipo di reperibilità presente");
      Application.index();
    }

    final PersonReperibilityType reperibilitySelected = 
        reperibility.id != null ? reperibility : reperibilities.get(0);


    rules.checkIfPermitted(reperibilitySelected);

    render(reperibilities, reperibilitySelected, currentDate);
  }

  /**
   * ritorna la lista di persone associate alla reperibilità nel periodo passato come parametro.
   *
   * @param reperibilityId l'id dell'attività di cui ritornare la lista di personale associato
   * @param start la data di inizio da considerare
   * @param end la data di fine da considerare
   */
  public static void reperibilityPeople(long reperibilityId, LocalDate start, LocalDate end) {

    PersonReperibilityType reperibility = 
        reperibilityDao.getPersonReperibilityTypeById(reperibilityId);
    if (reperibility != null) {
      rules.checkIfPermitted(reperibility);
      final List<PersonReperibility> people = 
          reperibilityManager2.reperibilityWorkers(reperibility, start, end);
      int index = 0;
      final List<ReperibilityEvent> reperibilityWorkers = new ArrayList<>();

      for (PersonReperibility personReperibility : people) {
        final EventColor eventColor = EventColor.values()[index % (EventColor.values().length - 1)];
        final Person person = personReperibility.person;
        final ReperibilityEvent event = ReperibilityEvent.builder()
            .allDay(true)
            .title(person.fullName())
            .personId(person.id)
            .eventColor(eventColor)
            .color(eventColor.backgroundColor)
            .textColor(eventColor.textColor)
            .borderColor(eventColor.borderColor)
            .className("removable")
            .mobile(person.mobile)
            .email(person.email)
            .build();
        reperibilityWorkers.add(event);
        index++;
      }
      reperibilityWorkers.sort(Comparator.comparing(ReperibilityEvent::getTitle));
      render(reperibilityWorkers);
    }

  }

  /**
   * ritorna la lista di eventi presenti per l'attività nel periodo start/end.
   *
   * @param reperibilityId l'id dell'attività da ricercare
   * @param start la data di inizio del periodo
   * @param end la data di fine del periodo
   * @throws JsonProcessingException eccezione in caso di errore di creazione del json
   */
  public static void events(long reperibilityId, LocalDate start, LocalDate end)
      throws JsonProcessingException {

    List<ReperibilityEvent> events = new ArrayList<>();
    PersonReperibilityType reperibility = 
        reperibilityDao.getPersonReperibilityTypeById(reperibilityId);
    if (reperibility != null && rules.check(reperibility)) {

      List<PersonReperibility> people = 
          reperibilityManager2.reperibilityWorkers(reperibility, start, end);

      int index = 0;

      // prende i turni associati alle persone attive in quel turno
      for (PersonReperibility personReperibility : people) {
        final Person person = personReperibility.person;
        final EventColor eventColor = EventColor.values()[index % (EventColor.values().length - 1)];
        events.addAll(reperibilityEvents(reperibility, person, start, end, eventColor));
        events.addAll(absenceEvents(person, start, end));
        index++;
      }
    }

    // Usato il jackson per facilitare la serializzazione dei LocalDate
    renderJSON(mapper.writeValueAsString(events));
  }

  /**
   * Chiamata dal fullCalendar dei turni per ogni evento di drop di un turno sul calendario.
   * Controlla se il turno passato come parametro può essere salvato in un dato giorno
   * ed eventualmente lo salva, altrimenti restituisce un errore
   *
   * @param personReperibilityDayId id del persnShiftDay da controllare
   * @param newDate giorno nel quale salvare il turno error 409 con messaggio di
   */
  public static void changeReperibility(long personReperibilityDayId, LocalDate newDate) {

    final PNotifyObject message;
    final Optional<PersonReperibilityDay> prd = 
        reperibilityDao.getPersonReperibilityDayById(personReperibilityDayId);
    if (prd.isPresent()) {
      final ReperibilityTypeMonth reperibilityTypeMonth = 
          reperibilityTypeMonthDao.byReperibilityTypeAndDate(
              prd.get().reperibilityType, newDate).orNull();
      if (rules.check(prd.get().reperibilityType) && rules.check(reperibilityTypeMonth)) {
        prd.get().date = newDate;
        Optional<String> error = reperibilityManager2.reperibilityPermitted(prd.get());
        if (error.isPresent()) {
          message = PNotifyObject.builder()
              .title("Errore")
              .hide(true)
              .text(error.get())
              .type("error").build();

          response.status = 409;
        } else {
          //salva il turno modificato
          reperibilityManager2.save(prd.get());

          message = PNotifyObject.builder()
              .title("Ok")
              .hide(true)
              .text(Web.msgModified(PersonShiftDay.class))
              .type("success").build();
        }
      } else {
        message = PNotifyObject.builder()
            .title("Forbidden")
            .hide(true)
            .text(Messages.get("forbidden"))
            .type("error").build();
        response.status = Http.StatusCode.FORBIDDEN;
      }

    } else {
      message = PNotifyObject.builder()
          .title("Error")
          .hide(true)
          .text(Messages.get("notFound"))
          .type("error").build();
      response.status = Http.StatusCode.NOT_FOUND;
    }

    renderJSON(message);
  }

  /**
   * inserisce un nuovo slot di turno per l'attività al turnista passati come parametro.
   *
   * @param personId l'id della persona in turno
   * @param date la data in cui inserire il turno
   * @param reperibilityId l'id dell'attività su cui inserire il reperibilie
   */
  public static void newReperibility(long personId, LocalDate date, long reperibilityId) {

    final PNotifyObject message;
    PersonReperibilityType reperibilityType = 
        reperibilityDao.getPersonReperibilityTypeById(reperibilityId);
    final ReperibilityTypeMonth reperibilityTypeMonth = 
        reperibilityTypeMonthDao.byReperibilityTypeAndDate(reperibilityType, date).orNull();
    if (reperibilityType != null) {
      if (rules.check(reperibilityType) && rules.check(reperibilityTypeMonth)) {
        Person person = personDao.getPersonById(personId);
        if (person == null) {
          message = PNotifyObject.builder()
              .title("Error")
              .hide(true)
              .text(Messages.get("notFound"))
              .type("error").build();
          response.status = Http.StatusCode.NOT_FOUND;
        } else {
          PersonReperibilityDay personReperibilityDay = new PersonReperibilityDay();
          personReperibilityDay.date = date;
          personReperibilityDay.reperibilityType = reperibilityType;
          personReperibilityDay.personReperibility = 
              reperibilityDao.getPersonReperibilityByPersonAndType(person, reperibilityType);
          Optional<String> error;
          if (validation.valid(personReperibilityDay).ok) {
            error = reperibilityManager2.reperibilityPermitted(personReperibilityDay);
          } else {
            error = Optional.of(Messages.get("validation.invalid"));
          }
          if (error.isPresent()) {
            response.status = 409;

            message = PNotifyObject.builder()
                .title("Errore")
                .hide(true)
                .text(error.get())
                .type("error").build();

          } else {
            reperibilityManager2.save(personReperibilityDay);

            message = PNotifyObject.builder()
                .title("Ok")
                .hide(true)
                .text(Web.msgCreated(PersonReperibilityDay.class))
                .type("success").build();
          }
        }


      } else {  // Le Drools non danno il grant
        message = PNotifyObject.builder()
            .title("Forbidden")
            .hide(true)
            .text(Messages.get("forbidden"))
            .type("error").build();
        response.status = Http.StatusCode.FORBIDDEN;
      }

    } else { // Il ReperibilityType specificato non esiste
      message = PNotifyObject.builder()
          .title("Error")
          .hide(true)
          .text(Messages.get("notFound"))
          .type("error").build();
      response.status = Http.StatusCode.NOT_FOUND;
    }

    renderJSON(message);           
  }

  /**
   * Effettua l'eliminazione di un turno.
   *
   * @param prd Reperibilità da eliminare
   */
  public static void deleteReperibility(PersonReperibilityDay prd) {
    final PNotifyObject message;
    if (prd == null) {
      message = PNotifyObject.builder()
          .title("Error")
          .hide(true)
          .text(Messages.get("notFound"))
          .type("error").build();
      response.status = Http.StatusCode.NOT_FOUND;
    } else {
      final ReperibilityTypeMonth reperibilityTypeMonth = 
          reperibilityTypeMonthDao.byReperibilityTypeAndDate(prd.reperibilityType, 
              prd.date).orNull();

      if (rules.check(prd.reperibilityType) && rules.check(reperibilityTypeMonth)) {

        reperibilityManager2.delete(prd);

        message = PNotifyObject.builder()
            .title("Ok")
            .hide(true)
            .text(Web.msgDeleted(PersonShiftDay.class))
            .type("success").build();
      } else {
        message = PNotifyObject.builder()
            .title("Forbidden")
            .hide(true)
            .text(Messages.get("forbidden"))
            .type("error").build();
        response.status = Http.StatusCode.FORBIDDEN;
      }
    }
    //
    renderJSON(message);
  }

  /**
   * Verifica se il calendario è modificabile o meno nella data richiesta.
   *
   * @param reperibilityId id dell'attività da verificare
   * @param start data relativa al mese da controllare
   * @return true se l'attività è modificabile nella data richiesta, false altrimenti.
   */
  public static boolean editable(long reperibilityId, @Required LocalDate start) {

    PersonReperibilityType reperibilityType = 
        reperibilityDao.getPersonReperibilityTypeById(reperibilityId);
    if (reperibilityType == null || Validation.hasErrors()) {
      return false;
    }
    final ReperibilityTypeMonth reperibilityTypeMonth = 
        reperibilityTypeMonthDao.byReperibilityTypeAndDate(reperibilityType, start).orNull();

    return rules.check(reperibilityType) && rules.check(reperibilityTypeMonth);

  }

  /**
   * Calcola le ore di turno effettuate in quel periodo per ciascuna persona dell'attività
   * specificata.
   *
   * @param reperibilityId id dell'attività di reperibilità
   * @param start data iniziale
   * @param end data finale.
   */
  public static void recap(long reperibilityId, LocalDate start, LocalDate end) {
    PersonReperibilityType reperibility = 
        reperibilityDao.getPersonReperibilityTypeById(reperibilityId);

    if (reperibility != null) {

      rules.checkIfPermitted(reperibility);
      Map<Person, Integer> workDaysReperibilityCalculatedCompetences = reperibilityManager2
          .calculateReperibilityWorkDaysCompetences(reperibility, start, end);
      Map<Person, Integer> holidaysReperibilityCalculatedCompetences = 
          reperibilityManager2.calculateReperibilityHolidaysCompetences(reperibility, start, end);

      final ReperibilityTypeMonth reperibilityTypeMonth = reperibilityTypeMonthDao
          .byReperibilityTypeAndDate(reperibility, start).orNull();

      render(workDaysReperibilityCalculatedCompetences, holidaysReperibilityCalculatedCompetences,
          reperibilityTypeMonth, reperibility, start);
    }
  }

  /**
   * DTO che modellano le assenze della persona nel periodo.
   *
   * @param person Persona della quale recuperare le assenze
   * @param start data iniziale del periodo
   * @param end data finale del periodo
   * @return Una lista di DTO che modellano le assenze di quella persona nell'intervallo specificato
   *     da renderizzare nel fullcalendar.
   */
  private static List<ReperibilityEvent> absenceEvents(Person person, 
      LocalDate start, LocalDate end) {

    final List<JustifiedTypeName> types = ImmutableList
        .of(JustifiedTypeName.all_day, JustifiedTypeName.assign_all_day, 
            JustifiedTypeName.complete_day_and_add_overtime);

    List<Absence> absences = absenceDao.filteredByTypes(person, start, end, types, 
        Optional.fromNullable(false), Optional.<Boolean>absent());
    List<ReperibilityEvent> events = new ArrayList<>();
    ReperibilityEvent event = null;

    for (Absence abs : absences) {

      /*
       * Per quanto riguarda gli eventi 'allDay':
       *
       * La convenzione del fullcalendar è quella di avere il parametro end = null
       * nel caso di eventi su un singolo giorno, mentre nel caso di evento su più giorni il
       * parametro end assume il valore del giorno successivo alla fine effettiva
       * (perchè ne imposta l'orario alla mezzanotte).
       */
      if (event == null
          || event.getEnd() == null && !event.getStart().plusDays(1)
          .equals(abs.personDay.date)
          || event.getEnd() != null && !event.getEnd().equals(abs.personDay.date)) {

        event = ReperibilityEvent.builder()
            .allDay(true)
            .title("Assenza di " + abs.personDay.person.fullName())
            .start(abs.personDay.date)
            .editable(false)
            .color(EventColor.RED.backgroundColor)
            .textColor(EventColor.RED.textColor)
            .borderColor(EventColor.RED.borderColor)
            .build();

        events.add(event);
      } else {
        event.setEnd(abs.personDay.date.plusDays(1));
      }

    }
    return events;
  }

  /**
   * Carica la lista delle reperibilità di un certo tipo associati ad una determinata persona in
   * un intervallo di tempo.
   *
   * @param reperibility attività di reperibilità
   * @param person persona associata ai turni
   * @param start data inizio intervallo di tempo
   * @param end data fine intervallo di tempo
   * @param color colore da utilizzare per il rendering degli eventi restituiti
   * @return Una lista di DTO da serializzare in Json per renderizzarli nel fullcalendar.
   */
  private static List<ReperibilityEvent> reperibilityEvents(PersonReperibilityType reperibility, 
      Person person, LocalDate start, LocalDate end, EventColor color) {

    return reperibilityDao.getPersonReperibilityDaysByPeriodAndType(start, 
        end, reperibility, person).stream()
        .map(personReperibilityDay -> {
          final ReperibilityEvent event = ReperibilityEvent.builder()

              .personReperibilityDayId(personReperibilityDay.id)
              .title(person.fullName())
              .start(personReperibilityDay.date)
              .end(personReperibilityDay.date)
              .durationEditable(false)
              .color(color.backgroundColor)
              .textColor(color.textColor)
              .borderColor(color.borderColor)
              .className("removable")
              .build();

          return event;
        }).collect(Collectors.toList());
  }

  /**
   * ritorna informazioni alla vista relative ai dipendenti associati all'attività mensile
   * e alle ore già approvate/pagate relative all'attività stessa.
   *
   * @param reperibilityId l'id dell'attività per cui ricercare le approvazioni
   * @param date la data da cui ricercare le approvazioni
   */
  public static void monthReperibilityApprovement(long reperibilityId, @Required LocalDate date) {
    if (Validation.hasErrors()) {
      notFound();
    }

    PersonReperibilityType reperibility = 
        reperibilityDao.getPersonReperibilityTypeById(reperibilityId);
    notFoundIfNull(reperibility);
    rules.checkIfPermitted(reperibility);
    final YearMonth monthToApprove = new YearMonth(date);

    final Optional<ReperibilityTypeMonth> monthStatus = 
        reperibilityTypeMonthDao.byReperibilityTypeAndDate(reperibility, date);
    final ReperibilityTypeMonth reperibilityTypeMonth;
    if (monthStatus.isPresent()) {
      reperibilityTypeMonth = monthStatus.get();
    } else {
      reperibilityTypeMonth = new ReperibilityTypeMonth();
      reperibilityTypeMonth.personReperibilityType = reperibility;
      reperibilityTypeMonth.yearMonth = monthToApprove;
      reperibilityTypeMonth.save();
    }
    final LocalDate monthbegin = monthToApprove.toLocalDate(1);
    final LocalDate monthEnd = monthbegin.dayOfMonth().withMaximumValue();
    final LocalDate today = LocalDate.now();

    final LocalDate lastDay;

    if (monthEnd.isAfter(today)) {
      lastDay = today;
    } else {
      lastDay = monthEnd;
    }
    List<WorkDaysReperibilityDto> listWorkdaysRep = Lists.newArrayList();
    List<HolidaysReperibilityDto> listHolidaysRep = Lists.newArrayList();
    final List<Person> people = reperibilityManager2
        .involvedReperibilityWorkers(reperibility, monthbegin, monthEnd);

    CompetenceCode workDayActivity = reperibility.monthlyCompetenceType.workdaysCode;        
    CompetenceCode holidayActivity = reperibility.monthlyCompetenceType.holidaysCode;

    people.forEach(person -> {
      WorkDaysReperibilityDto dto = new WorkDaysReperibilityDto();

      dto.person = person;
      dto.workdaysReperibility = reperibilityManager2
          .calculatePersonReperibilityCompetencesInPeriod(reperibility, person,
              monthbegin, lastDay, workDayActivity);
      dto.workdaysPeriods = reperibilityManager2
          .getReperibilityPeriod(person, monthbegin, monthEnd, reperibility, false);
      HolidaysReperibilityDto dtoHoliday = new HolidaysReperibilityDto();
      dtoHoliday.person = person;
      dtoHoliday.holidaysReperibility = reperibilityManager2
          .calculatePersonReperibilityCompetencesInPeriod(reperibility, person,
              monthbegin, lastDay, holidayActivity);
      dtoHoliday.holidaysPeriods = reperibilityManager2
          .getReperibilityPeriod(person, monthbegin, monthEnd, reperibility, true);

      listWorkdaysRep.add(dto);
      listHolidaysRep.add(dtoHoliday);
    });

    //TODO: nella render ritornare una lista di dto alla vista
    render(reperibilityTypeMonth, listWorkdaysRep, listHolidaysRep);

  }


  /**
   * approva le quantità giornaliere di reperibilità nel mese.
   *
   * @param version la versione da verificare
   * @param reperibilityTypeMonthId l'id del reperibilityTypeMonth da controllare
   */
  public static void approveReperibilityInMonth(long version, long reperibilityTypeMonthId) {

    ReperibilityTypeMonth reperibilityTypeMonth = 
        reperibilityTypeMonthDao.byId(reperibilityTypeMonthId).orNull();
    notFoundIfNull(reperibilityTypeMonth);
    rules.checkIfPermitted(reperibilityTypeMonth);
    Map<String, Object> args = new HashMap<>();
    if (reperibilityTypeMonth.version != version) {
      flash.error("Le reperibilità sono cambiate rispetto al riepilogo mostrato."
          + "Il nuovo riepilogo è stato ricalcolato");
      flash.keep();
      args.put("date", reperibilityTypeMonth.yearMonth.toLocalDate(1).toString());
      args.put("reperibilityId", reperibilityTypeMonth.personReperibilityType.id);
      redirect(Router.reverse("ReperibilityCalendar.monthReperibilityApprovement", args).url);
    }
    reperibilityTypeMonth.approved = true;
    reperibilityTypeMonth.save();
    //TODO: completare questo metodo nel reperibility manager
    reperibilityManager2.assignReperibilityCompetences(reperibilityTypeMonth);
    args.put("date", TemplateExtensions.format(reperibilityTypeMonth.yearMonth.toLocalDate(1)));
    args.put("activity.id", reperibilityTypeMonth.personReperibilityType.id);
    redirect(Router.reverse("ReperibilityCalendar.show", args).url);

  }

  /**
   * permette la rimozione dell'approvazione per i giorni di reperibilità.
   *
   * @param reperibilityTypeMonthId l'id del reperibilityTypeMonth contenente le info 
   *     su approvazione della reperibilità
   */
  public static void removeApprovation(long reperibilityTypeMonthId) {

    ReperibilityTypeMonth reperibilityTypeMonth = 
        reperibilityTypeMonthDao.byId(reperibilityTypeMonthId).orNull();
    notFoundIfNull(reperibilityTypeMonth);
    rules.checkIfPermitted(reperibilityTypeMonth);
    reperibilityTypeMonth.approved = false;
    reperibilityTypeMonth.save();

    Map<String, Object> args = new HashMap<>();
    args.put("date", TemplateExtensions.format(reperibilityTypeMonth.yearMonth.toLocalDate(1)));
    args.put("activity.id", reperibilityTypeMonth.personReperibilityType.id);
    redirect(Router.reverse("ReperibilityCalendar.show", args).url);
  }

}
