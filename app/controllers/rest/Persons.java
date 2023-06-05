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

package controllers.rest;

import cnr.sync.dto.CompetenceDto;
import cnr.sync.dto.DayRecap;
import cnr.sync.dto.SimplePersonDto;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import common.security.SecurityRules;
import controllers.Resecure;
import controllers.Resecure.BasicAuth;
import controllers.Security;
import dao.AbsenceDao;
import dao.CompetenceDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.wrapper.IWrapperFactory;
import helpers.JsonResponse;
import it.cnr.iit.epas.DateInterval;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.PersonDayManager;
import models.Competence;
import models.Office;
import models.Person;
import models.User;
import org.joda.time.LocalDate;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Controller per la visualizzazione via REST di alcuni dati dei dipendenti.
 */
@With(Resecure.class)
@Slf4j
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
  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static SecurityRules rules;

  /**
   * Ritorna le informazioni sui giorni nel periodo richiesto per la persona identificata
   * dal parametro email.
   *
   * @param email la mail della persona di cui si vogliono le informazioni sui giorni
   * @param start l'inizio del periodo
   * @param end la fine del periodo
   */
  @BasicAuth
  public static void days(String email, LocalDate start, LocalDate end) {

    if (email == null) {
      JsonResponse.badRequest("Email obbligatoria");
    }
    Optional<Person> person = personDao.byEmail(email);
    if (!person.isPresent()) {
      JsonResponse.notFound("Indirizzo email incorretto. Non è presente la "
          + "mail cnr che serve per la ricerca. Assicurarsi di aver"
          + "lanciato il job per la sincronizzazione delle email dei dipendenti");
    }
    if (start == null || end == null || start.isAfter(end)) {
      JsonResponse.badRequest("Date non valide");
    }

    List<DayRecap> personDays = FluentIterable.from(personDao.getPersonDayIntoInterval(
        person.get(), new DateInterval(start, end), false))
        .transform(personday -> {
          DayRecap dayRecap = new DayRecap();

          dayRecap.workingMinutes = personday.getAssignableTime();
          dayRecap.date = personday.getDate().toString();
          dayRecap.mission = personDayManager.isOnMission(personday);
          dayRecap.workingTime =
              wrapperFactory.create(personday).getWorkingTimeTypeDay().get().getWorkingTime();
          return dayRecap;
        }).toList();

    renderJSON(personDays);
  }

  /**
   * Ritorna il json contenente le missioni presenti nel periodo da start a end per la persona 
   * identificata dalla mail email.
   *
   * @param email la mail che identifica la persona
   * @param start la data di inizio della ricerca
   * @param end la data di fine della ricerca
   * @param forAttachment se occorre aggiungere gli allegati
   */
  @BasicAuth
  public static void missions(String email, LocalDate start, LocalDate end, boolean forAttachment) {

    if (email == null) {
      JsonResponse.badRequest("Email obbligatoria");
    }
    Optional<Person> person = personDao.byEmail(email);
    List<DayRecap> personDays = Lists.newArrayList();
    if (person.isPresent()) {

      personDays = FluentIterable.from(absenceDao.getAbsencesInPeriod(
          person, start, Optional.fromNullable(end), forAttachment))
          .transform(absence -> {
            DayRecap dayRecap = new DayRecap();
            dayRecap.workingMinutes = 0;
            dayRecap.date = absence.getPersonDay().getDate().toString();
            if (personDayManager.isOnMission(absence.getPersonDay())) {
              dayRecap.mission = true;
            } else {
              dayRecap.mission = false;
            }
            return dayRecap;
          }).toList();
    }
    renderJSON(personDays);
  }

  /**
   * Il json contenente le competenze assegnate nel periodo da start a end relative ai codici 
   * presenti nella lista code per la persona identificata dalla mail email.
   *
   * @param email la mail che identifica la persona
   * @param start la data di inizio da cui cercare
   * @param end la data di fine fino a cui cercare
   * @param code la lista dei codici di competenza da ricercare
   */
  @BasicAuth
  public static void competences(String email, LocalDate start, LocalDate end, List<String> code) {
    if (email == null) {
      JsonResponse.badRequest("Email obbligatoria");
    }
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

  /**
   * Ritorna la lista delle persone appartenenti alla sede sedeId alla data date.
   *
   * @param sedeId l'identificativo della sede di cui si vogliono le persone
   * @param date la data a cui si vuole la lista
   */
  @BasicAuth
  public static void peopleList(String sedeId, LocalDate date) {
    User user = Security.getUser().get();
    log.info("Utente {} loggato correttamente", user.getUsername());
    if (Strings.isNullOrEmpty(sedeId)) {
      JsonResponse.badRequest("Identificativo di sede nullo");
    }
    if (date == null) {
      JsonResponse.badRequest("Data nulla");
    }
    Optional<Office> office = officeDao.byCodeId(sedeId);
    if (!office.isPresent()) {
      JsonResponse.notFound("Sede non presente in anagrafica");
    } 
    rules.checkIfPermitted(office.get());
    Set<Office> offices = Sets.newHashSet();
    offices.add(office.get());
    List<Person> list = personDao.list(Optional.<String>absent(), offices, 
        false, date, date, true).list();
    List<SimplePersonDto> personList = FluentIterable.from(list)
        .transform(SimplePersonDto.FromPerson.ISTANCE).toList();
    renderJSON(personList);

  }
}