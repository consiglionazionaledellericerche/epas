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

package jobs;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import dao.AbsenceDao;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.inject.Inject;
import manager.recaps.charts.RenderResult;
import manager.recaps.charts.ResultFromFile;
import models.Person;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.enumerate.CheckType;
import org.joda.time.LocalDate;
import play.jobs.Job;

/**
 * Job che sistema assenze varie.
 *
 * @author Daniele Murgia
 * @since 05/07/16
 */
public class ChartJob extends Job<List<RenderResult>> {

  private Person person;
  private List<ResultFromFile> list;

  @Inject
  static AbsenceDao absenceDao;

  /**
   * Costruttore.
   *
   * @param person Persona
   * @param list   Lista delle assenze estratte dallo schedone annuale delle assenze.
   */
  public ChartJob(Person person, List<ResultFromFile> list) {
    super();
    this.person = person;
    this.list = list;
  }

  /**
   * La lista di renderResult  delle assenze parsate dallo schedone delle assenze annuali.
   *
   * @return La lista delle assenze parsate dallo schedone delle assenze annuali.
   */
  public List<RenderResult> doJobWithResult() {
    List<RenderResult> resultList = Lists.newArrayList();

    // ordino per data e rimuovo i duplicati
    final List<ResultFromFile> absencesParsed = list.stream().distinct()
        .sorted((resultFromFile, t1) -> resultFromFile.dataAssenza.compareTo(t1.dataAssenza))
        .collect(Collectors.toList());

    LocalDate dateFrom = absencesParsed.get(0).dataAssenza;
    LocalDate dateTo = absencesParsed.get(absencesParsed.size() - 1).dataAssenza;

    List<Absence> absences = absenceDao.findByPersonAndDate(person,
        dateFrom, Optional.fromNullable(dateTo), Optional.<AbsenceType>absent()).list();

    absencesParsed.forEach(item -> {
      RenderResult result = null;
      List<Absence> values = absences
          .stream()
          .filter(r -> r.getPersonDay().getDate().isEqual(item.dataAssenza))
          .collect(Collectors.toList());
      if (!values.isEmpty()) {
        final Predicate<Absence> compareCode = a -> a.getAbsenceType().getCode()
            .equalsIgnoreCase(item.codice)
            || a.getAbsenceType().getCertificateCode().equalsIgnoreCase(item.codice);
        if (values.stream().anyMatch(compareCode)) {
          result = new RenderResult(null, person.getNumber(), person.getName(),
              person.getSurname(), item.codice, item.dataAssenza, true, "Ok",
              values.stream().filter(compareCode)
                  .findFirst().get().getAbsenceType().getCode(), CheckType.SUCCESS);
        } else {
          result = new RenderResult(null, person.getNumber(), person.getName(),
              person.getSurname(), item.codice, item.dataAssenza, false,
              "Mismatch tra assenza trovata e quella dello schedone",
              values.stream().findFirst().get().getAbsenceType().getCode(), CheckType.WARNING);
        }
      } else {
        result = new RenderResult(null, person.getNumber(), person.getName(),
            person.getSurname(), item.codice, item.dataAssenza, false,
            "Nessuna assenza per il giorno", null, CheckType.DANGER);
      }
      resultList.add(result);
    });

    return resultList;
  }
}
