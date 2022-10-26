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
import com.google.common.collect.Sets;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.PersonShiftDayDao;
import dao.history.HistoryValue;
import dao.history.PersonDayHistoryDao;
import dao.history.StampingHistoryDao;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.val;
import manager.recaps.personstamping.PersonStampingDayRecap;
import manager.recaps.personstamping.PersonStampingRecap;
import models.Person;
import models.PersonDay;
import models.PersonShiftDay;
import models.Stamping;
import models.User;
import models.dto.OffSiteWorkingTemp;
import org.joda.time.LocalDate;
import org.testng.collections.Maps;

/**
 * Contiene metodi di utilità per la generazione del PDF
 * con il resoconto mensile delle presenze/assenze di un 
 * dipendente.
 */
public class PrintTagsManager {

  private final StampingHistoryDao stampingHistoryDao;
  private final PersonDayDao personDayDao;
  private final PersonDayHistoryDao personDayHistoryDao;
  private final PersonDao personDao;
  private final PersonShiftDayDao personShiftDayDao;

  /**
   * Injection.
   *
   * @param stampingHistoryDao il dao dello storico delle timbrature
   * @param personDayHistoryDao il dao dello storico dei personday
   * @param personDayDao il dao dei personday
   * @param personDao il dao delle persone
   * @param personShiftDayDao il dao dei giorni di turno
   */
  @Inject
  public PrintTagsManager(
      StampingHistoryDao stampingHistoryDao, PersonDayHistoryDao personDayHistoryDao,
      PersonDayDao personDayDao, PersonDao personDao, PersonShiftDayDao personShiftDayDao) {
    this.stampingHistoryDao = stampingHistoryDao;
    this.personDayHistoryDao = personDayHistoryDao;
    this.personDayDao = personDayDao;
    this.personDao = personDao;
    this.personShiftDayDao = personShiftDayDao;
  }

  /**
   * Metodo che ritorna le informazioni sullo storico delle timbrature del dipendente.
   *
   * @param psDto il person stamping recap delle timbrature del dipendente
   * @return la lista di liste contenente le informazioni sullo storico delle timbrature.
   */
  public List<List<HistoryValue<Stamping>>> getHistoricalList(PersonStampingRecap psDto) {
    List<List<HistoryValue<Stamping>>> historyStampingsList = Lists.newArrayList();

    for (PersonStampingDayRecap day : psDto.daysRecap) {
      if (!day.ignoreDay) {
        for (Stamping stamping : day.personDay.getStampings()) {
          if (stamping.isMarkedByAdmin()) {
            historyStampingsList.add(stampingHistoryDao.stampings(stamping.id));
          }
        }
      }
    }
    return historyStampingsList;
  }

  /**
   * Metodo che ritorna le informazioni sulle timbrature fuori sede.
   *
   * @param psDto il personStampingRecap contenente il recap delle timbrature del dipendente
   * @return la lista contenente tutte le timbrature fuori sede per la persona.
   */
  public List<OffSiteWorkingTemp> getOffSiteStampings(PersonStampingRecap psDto) {
    List<OffSiteWorkingTemp> list = Lists.newArrayList();
    for (PersonStampingDayRecap day : psDto.daysRecap) {
      if (!day.ignoreDay) {
        for (Stamping stamping : day.personDay.getStampings()) {
          if (stamping.isOffSiteWork()) {
            OffSiteWorkingTemp temp = new OffSiteWorkingTemp();
            temp.date = stamping.getPersonDay().getDate();
            temp.stamping = stamping;
            temp.reason = stamping.getReason();
            temp.place = stamping.getPlace();
            temp.note = stamping.getNote();
            list.add(temp);
          }            
        }
      }
    }

    return list;
  }

  /**
   * Metodo che ritorna la mappa utente-insieme di date contenente le info su chi ha fatto 
   * le timbrature per ciascun giorno.
   *
   * @param person la persona di cui considerare le timbrature
   * @param yearMonth l'anno/mese di riferimento
   * @return la mappa di utente-set di giorni contenente le informazioni su chi ha fatto le 
   *     timbrature per ciascun giorno.
   */
  public Map<User, Set<LocalDate>> getStampingOwnerInDays(Person person, YearMonth yearMonth) {
    Verify.verifyNotNull(person);
    Verify.verifyNotNull(person.id, "L'id della persona %s è null", person);

    val p = personDao.getPersonById(person.id);
    val personDays = personDayDao.getPersonDayInPeriod(
        p, new LocalDate(yearMonth.getYear(), yearMonth.getMonthValue(), 1), 
        Optional.of(new LocalDate(yearMonth.getYear(), yearMonth.getMonthValue(), 1)
            .dayOfMonth().withMaximumValue()));
    Map<User, Set<LocalDate>> badgeReaderMap = Maps.newHashMap();
    personDays.forEach(pd -> {
      personDayHistoryDao.stampingsAtCreation(pd.id).forEach(s -> {
        if (!badgeReaderMap.containsKey(s.revision.owner)) {
          Set<LocalDate> dates = Sets.newHashSet(s.value.getDate().toLocalDate());
          badgeReaderMap.put(s.revision.owner, dates);
        } else {
          badgeReaderMap.get(s.revision.owner).add(s.value.getDate().toLocalDate());
        }
      });
    });
    return badgeReaderMap;
  }

  /**
   * Ritorna La lista dei giorni festivi lavorati in turno.
   *
   * @param psDto il personStampingRecap contenente le informazioni del mese
   * @return la lista di giorni festivi lavorati in turno.
   */
  public List<PersonDay> getHolidaysInShift(PersonStampingRecap psDto) {
    List<PersonDay> days = Lists.newArrayList();
    LocalDate from = new LocalDate(psDto.year, psDto.month, 1);
    LocalDate to = from.dayOfMonth().withMaximumValue();
    List<PersonShiftDay> personShiftDays = personShiftDayDao.listByPeriod(psDto.person, from, to);
    if (personShiftDays.isEmpty()) {
      return days;
    }
    psDto.daysRecap.forEach(day -> {
      if (day.personDay.isHoliday()) {
        personShiftDays.forEach(psd -> { 
          if (psd.getDate().isEqual(day.personDay.getDate())) {
            days.add(day.personDay);
          }
        });
      }
    });
    return days;
  }
}