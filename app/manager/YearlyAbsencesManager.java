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
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import dao.AbsenceDao;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import models.Person;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.JustifiedType.JustifiedTypeName;
import org.joda.time.LocalDate;

/**
 * Managerr per la gestione delle YearlyAbsences.
 */
@Slf4j
public class YearlyAbsencesManager {

  /**
   * Comparatore dei nomi di persona (ignorando il case).
   */
  public Comparator<Person> personNameComparator = new Comparator<Person>() {

    public int compare(Person person1, Person person2) {

      String name1 = person1.getSurname().toUpperCase();
      String name2 = person2.getSurname().toUpperCase();

      if (name1.equals(name2)) {
        return person1.getName().toUpperCase().compareTo(person2.getName().toUpperCase());
      }
      return name1.compareTo(name2);

    }

  };

  /**
   * Comparatore dei codice di un absenceCode.
   */
  public Comparator<AbsenceType> absenceCodeComparator = new Comparator<AbsenceType>() {
    public int compare(AbsenceType absenceCode1, AbsenceType absenceCode2) {
      return absenceCode1.getCode().compareTo(absenceCode2.getCode());
    }

  };

  @Inject
  private AbsenceDao absenceDao;

  /**
   * Genera la tabella con la tripla persona-tipo assenza-quantità.
   *
   * @param persons la lista delle persone
   * @param abt il tipo di assenza
   * @param begin la data di inizio
   * @param end la data di fine
   * @return la tabella contenente la tripla persona-tipo assenza-quantità.
   */
  public Table<Person, AbsenceType, Integer> populateMonthlyAbsencesTable(
      List<Person> persons, AbsenceType abt, LocalDate begin, LocalDate end) {

    Table<Person, AbsenceType, Integer> tableMonthlyAbsences =
        TreeBasedTable.create(personNameComparator, absenceCodeComparator);
    for (Person p : persons) {
      List<Absence> absenceInMonth =
          absenceDao.getAbsenceByCodeInPeriod(
              Optional.fromNullable(p), Optional.<String>absent(), begin, end,
              Optional.<JustifiedTypeName>absent(), false, false);

      tableMonthlyAbsences.put(p, abt, absenceInMonth.size());
      for (Absence abs : absenceInMonth) {
        Integer value = tableMonthlyAbsences.row(p).get(abs.getAbsenceType());
        log.trace("Per la persona {} il codice {} vale: {}",
            new Object[]{p, abs.getAbsenceType().getCode(), value});
        if (value == null) {
          log.trace("Inserisco in tabella nuova assenza per {} con codice {}",
              p, abs.getAbsenceType().getCode());
          tableMonthlyAbsences.row(p).put(abs.getAbsenceType(), 1);
        } else {
          tableMonthlyAbsences.row(p).put(abs.getAbsenceType(), value + 1);
          log.trace("Incremento il numero di giorni per l'assenza {} di {} al valore {}",
              new Object[]{abs.getAbsenceType().getCode(), p, value + 1});

        }
      }
    }
    return tableMonthlyAbsences;
  }

}