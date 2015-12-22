package controllers.rest;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

import cnr.sync.dto.CompetenceDto;
import cnr.sync.dto.DayRecap;

import controllers.Resecure;
import controllers.Resecure.BasicAuth;

import dao.AbsenceDao;
import dao.CompetenceDao;
import dao.PersonDao;
import dao.wrapper.IWrapperFactory;

import helpers.JsonResponse;

import it.cnr.iit.epas.DateInterval;

import manager.PersonDayManager;

import models.Absence;
import models.Competence;
import models.Person;
import models.PersonDay;

import org.joda.time.LocalDate;

import play.mvc.Controller;
import play.mvc.With;

import java.util.List;

import javax.inject.Inject;

@With(Resecure.class)
public class Persons extends Controller {

  @Inject
  private static PersonDao personDao;
  @Inject
  private static PersonDayManager personDayManager;
  @Inject
  private static IWrapperFactory wrapperFactory;
  @Inject
  private static AbsenceDao absenceDao;
  @Inject
  private static CompetenceDao competenceDao;

  @BasicAuth
  public static void days(String email, LocalDate start, LocalDate end) {

    //Person person = personDao.getPersonByPerseoId(perseoId);
    Person person = personDao.getPersonByEmail(email);
    if (person == null) {
      JsonResponse.notFound("Indirizzo email incorretto. Non è presente la "
              + "mail cnr che serve per la ricerca. Assicurarsi di aver"
              + "lanciato il job per la sincronizzazione delle email dei dipendenti");
    }
    if (start == null || end == null || start.isAfter(end)) {
      JsonResponse.badRequest("Date non valide");
    }

    List<DayRecap> personDays = FluentIterable.from(personDao.getPersonDayIntoInterval(
            person, new DateInterval(start, end), false))
            .transform(new Function<PersonDay, DayRecap>() {
              @Override
              public DayRecap apply(PersonDay personday) {
                DayRecap dayRecap = new DayRecap();
                dayRecap.workingMinutes = personDayManager.workingMinutes(wrapperFactory.create(personday));
                dayRecap.date = personday.date.toString();
                dayRecap.mission = personDayManager.isOnMission(personday);
                dayRecap.workingTime = wrapperFactory.create(personday).getWorkingTimeTypeDay().get().workingTime;

                return dayRecap;
              }
            }).toList();

    renderJSON(personDays);
  }

  @BasicAuth
  public static void missions(String email, LocalDate start, LocalDate end, boolean forAttachment) {

    //Person person = personDao.getPersonByPerseoId(perseoId);
    Person person = personDao.getPersonByEmail(email);
    List<DayRecap> personDays = Lists.newArrayList();
    if (person != null) {

      personDays = FluentIterable.from(
              absenceDao.getAbsencesInPeriod(Optional.fromNullable(person), start, Optional.fromNullable(end), forAttachment))
              .transform(new Function<Absence, DayRecap>() {
                @Override
                public DayRecap apply(Absence absence) {
                  DayRecap dayRecap = new DayRecap();
                  dayRecap.workingMinutes = 0;
                  dayRecap.date = absence.personDay.date.toString();
                  if (personDayManager.isOnMission(absence.personDay)) {
                    dayRecap.mission = true;
                  } else {
                    dayRecap.mission = false;
                  }
                  return dayRecap;
                }
              }).toList();
    }
    renderJSON(personDays);
  }

  @BasicAuth
  public static void competences(String email, LocalDate start, LocalDate end, List<String> code) {

    Person person = personDao.byEmail(email).orNull();
    if (person == null) {
      JsonResponse.notFound("Indirizzo email incorretto");
    }
    if (start == null || end == null || start.isAfter(end)) {
      JsonResponse.badRequest("Date non valide");
    }

    List<Competence> competences = Lists.newArrayList();

    while (!start.isAfter(end)) {

      competences.addAll(competenceDao.competenceInMonth(person, start.getYear(),
              start.getMonthOfYear(), Optional.fromNullable(code)));

      start = start.plusMonths(1);
      // Il caso in cui non vengano specificate delle date che coincidono con l'inizio e la fine
      // di un mese
      if (start.isAfter(end) && start.getMonthOfYear() == end.getMonthOfYear()) {
        competences.addAll(competenceDao.competenceInMonth(person, start.getYear(),
                start.getMonthOfYear(), Optional.fromNullable(code)));
      }
    }

    List<CompetenceDto> competencesList = FluentIterable.from(competences)
            .transform(CompetenceDto.FromCompetence.ISTANCE).toList();

    renderJSON(competencesList);
  }
}
