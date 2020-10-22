package manager;

import com.google.common.base.Optional;
import dao.AbsenceDao;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import models.Person;
import models.absences.Absence;
import models.exports.PersonEmailFromJson;
import models.exports.PersonPeriodAbsenceCode;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbsenceFromJsonManager {

  private static final Logger log = LoggerFactory.getLogger(AbsenceFromJsonManager.class);
  @Inject
  private AbsenceDao absenceDao;

  /**
   * La lista dei personPeriodAbsenceCode relativa ai parametri passati.
   * @param body il dto arrivato via json
   * @param dateFrom la data da cui cercare
   * @param dateTo la data fino a cui cercare
   * @return la lista dei PersonPeriodAbsenceCode nel periodo compreso tra 'dateFrom' e 'dateTo'
   *     per le persone recuperate dal 'body' contenente la lista delle persone ricavate dalle
   *     email arrivate via chiamata post json.
   */
  public List<PersonPeriodAbsenceCode> getPersonForAbsenceFromJson(
      PersonEmailFromJson body, LocalDate dateFrom, LocalDate dateTo) {
    List<PersonPeriodAbsenceCode> personsToRender = new ArrayList<PersonPeriodAbsenceCode>();
    PersonPeriodAbsenceCode personPeriodAbsenceCode = null;

    String meseInizio = "";
    String meseFine = "";
    String giornoInizio = "";
    String giornoFine = "";
    for (Person person : body.persons) {
      personPeriodAbsenceCode = new PersonPeriodAbsenceCode();
      if (person != null) {
        log.debug("Controllo {}", person.getFullname());

        List<Absence> absences =
            absenceDao.getAbsencesInPeriod(
                Optional.fromNullable(person), dateFrom, Optional.fromNullable(dateTo), false);

        log.debug("Lista assenze per {}: {}", person.getFullname(), absences);

        LocalDate startCurrentPeriod = null;
        LocalDate endCurrentPeriod = null;
        Absence previousAbsence = null;
        for (Absence abs : absences) {

          if (previousAbsence == null) {
            previousAbsence = abs;
            startCurrentPeriod = abs.personDay.date;
            endCurrentPeriod = abs.personDay.date;
            continue;
          }
          if (abs.absenceType.code.equals(previousAbsence.absenceType.code)) {
            if (!endCurrentPeriod.isEqual(abs.personDay.date.minusDays(1))) {
              personPeriodAbsenceCode = new PersonPeriodAbsenceCode();
              personPeriodAbsenceCode.personId = person.id;
              personPeriodAbsenceCode.name = person.name;
              personPeriodAbsenceCode.surname = person.surname;
              personPeriodAbsenceCode.code = previousAbsence.absenceType.code;
              if (startCurrentPeriod.getMonthOfYear() < 10) {
                meseInizio = "0" + startCurrentPeriod.getMonthOfYear();
              } else {
                meseInizio = String.valueOf(startCurrentPeriod.getMonthOfYear());
              }
              if (endCurrentPeriod.getMonthOfYear() < 10) {
                meseFine = "0" + endCurrentPeriod.getMonthOfYear();
              } else {
                meseFine = String.valueOf(endCurrentPeriod.getMonthOfYear());
              }

              if (startCurrentPeriod.getDayOfMonth() < 10) {
                giornoInizio = "0" + startCurrentPeriod.getDayOfMonth();
              } else {
                giornoInizio = "" + String.valueOf(startCurrentPeriod.getDayOfMonth());
              }
              if (endCurrentPeriod.getDayOfMonth() < 10) {
                giornoFine = "0" + endCurrentPeriod.getDayOfMonth();
              } else {
                giornoFine = String.valueOf(endCurrentPeriod.getDayOfMonth());
              }
              personPeriodAbsenceCode.start =
                  startCurrentPeriod.getYear() + "-" + meseInizio + "-" + giornoInizio;
              personPeriodAbsenceCode.end =
                  endCurrentPeriod.getYear() + "-" + meseFine + "-" + giornoFine;
              personsToRender.add(personPeriodAbsenceCode);

              previousAbsence = abs;
              startCurrentPeriod = abs.personDay.date;
              endCurrentPeriod = abs.personDay.date;
              continue;

            } else {
              endCurrentPeriod = abs.personDay.date;
              continue;
            }
          } else {
            personPeriodAbsenceCode = new PersonPeriodAbsenceCode();
            personPeriodAbsenceCode.personId = person.id;
            personPeriodAbsenceCode.name = person.name;
            personPeriodAbsenceCode.surname = person.surname;
            personPeriodAbsenceCode.code = previousAbsence.absenceType.code;

            if (startCurrentPeriod.getMonthOfYear() < 10) {
              meseInizio = "0" + startCurrentPeriod.getMonthOfYear();
            } else {
              meseInizio = String.valueOf(startCurrentPeriod.getMonthOfYear());
            }
            if (endCurrentPeriod.getMonthOfYear() < 10) {
              meseFine = "0" + endCurrentPeriod.getMonthOfYear();
            } else {
              meseFine = String.valueOf(endCurrentPeriod.getMonthOfYear());
            }

            if (startCurrentPeriod.getDayOfMonth() < 10) {
              giornoInizio = "0" + startCurrentPeriod.getDayOfMonth();
            } else {
              giornoInizio = String.valueOf(startCurrentPeriod.getDayOfMonth());
            }
            if (endCurrentPeriod.getDayOfMonth() < 10) {
              giornoFine = "0" + endCurrentPeriod.getDayOfMonth();
            } else {
              giornoFine = String.valueOf(endCurrentPeriod.getDayOfMonth());
            }
            personPeriodAbsenceCode.start =
                startCurrentPeriod.getYear() + "-" + meseInizio + "-" + giornoInizio;
            personPeriodAbsenceCode.end =
                endCurrentPeriod.getYear() + "-" + meseFine + "-" + giornoFine;
            personsToRender.add(personPeriodAbsenceCode);

            previousAbsence = abs;
            startCurrentPeriod = abs.personDay.date;
            endCurrentPeriod = abs.personDay.date;
          }
        }

        if (previousAbsence != null) {
          personPeriodAbsenceCode = new PersonPeriodAbsenceCode();
          personPeriodAbsenceCode.personId = person.id;
          personPeriodAbsenceCode.name = person.name;
          personPeriodAbsenceCode.surname = person.surname;
          personPeriodAbsenceCode.code = previousAbsence.absenceType.code;
          if (startCurrentPeriod.getMonthOfYear() < 10) {
            meseInizio = "0" + startCurrentPeriod.getMonthOfYear();
          } else {
            meseInizio = String.valueOf(startCurrentPeriod.getMonthOfYear());
          }
          if (endCurrentPeriod.getMonthOfYear() < 10) {
            meseFine = "0" + endCurrentPeriod.getMonthOfYear();
          } else {
            meseFine = String.valueOf(endCurrentPeriod.getMonthOfYear());
          }

          if (startCurrentPeriod.getDayOfMonth() < 10) {
            giornoInizio = "0" + startCurrentPeriod.getDayOfMonth();
          } else {
            giornoInizio = String.valueOf(startCurrentPeriod.getDayOfMonth());
          }
          if (endCurrentPeriod.getDayOfMonth() < 10) {
            giornoFine = "0" + endCurrentPeriod.getDayOfMonth();
          } else {
            giornoFine = String.valueOf(endCurrentPeriod.getDayOfMonth());
          }
          personPeriodAbsenceCode.start =
              startCurrentPeriod.getYear() + "-" + meseInizio + "-" + giornoInizio;
          personPeriodAbsenceCode.end =
              endCurrentPeriod.getYear() + "-" + meseFine + "-" + giornoFine;
          personsToRender.add(personPeriodAbsenceCode);
        }
      } else {
        log.error("Richiesta persona non presente in anagrafica. "
            + "Possibile sia un non strutturato.");
      }
    }
    return personsToRender;
  }
}
