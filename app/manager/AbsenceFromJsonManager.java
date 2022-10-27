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

/**
 * Manager per contenere metodi di utilità per prelevare ed importare
 * le informazioni sulle assenze da JSON.
 */
public class AbsenceFromJsonManager {

  private static final Logger log = LoggerFactory.getLogger(AbsenceFromJsonManager.class);
  @Inject
  private AbsenceDao absenceDao;

  /**
   * La lista dei personPeriodAbsenceCode relativa ai parametri passati.
   *
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
            startCurrentPeriod = abs.getPersonDay().getDate();
            endCurrentPeriod = abs.getPersonDay().getDate();
            continue;
          }
          if (abs.getAbsenceType().getCode().equals(previousAbsence.getAbsenceType().getCode())) {
            if (!endCurrentPeriod.isEqual(abs.getPersonDay().getDate().minusDays(1))) {
              personPeriodAbsenceCode = new PersonPeriodAbsenceCode();
              personPeriodAbsenceCode.personId = person.id;
              personPeriodAbsenceCode.name = person.getName();
              personPeriodAbsenceCode.surname = person.getSurname();
              personPeriodAbsenceCode.code = previousAbsence.getAbsenceType().getCode();
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
              startCurrentPeriod = abs.getPersonDay().getDate();
              endCurrentPeriod = abs.getPersonDay().getDate();
              continue;

            } else {
              endCurrentPeriod = abs.getPersonDay().getDate();
              continue;
            }
          } else {
            personPeriodAbsenceCode = new PersonPeriodAbsenceCode();
            personPeriodAbsenceCode.personId = person.id;
            personPeriodAbsenceCode.name = person.getName();
            personPeriodAbsenceCode.surname = person.getSurname();
            personPeriodAbsenceCode.code = previousAbsence.getAbsenceType().getCode();

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
            startCurrentPeriod = abs.getPersonDay().getDate();
            endCurrentPeriod = abs.getPersonDay().getDate();
          }
        }

        if (previousAbsence != null) {
          personPeriodAbsenceCode = new PersonPeriodAbsenceCode();
          personPeriodAbsenceCode.personId = person.id;
          personPeriodAbsenceCode.name = person.getName();
          personPeriodAbsenceCode.surname = person.getSurname();
          personPeriodAbsenceCode.code = previousAbsence.getAbsenceType().getCode();
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