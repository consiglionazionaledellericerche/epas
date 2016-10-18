package jobs;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import dao.AbsenceDao;

import lombok.extern.slf4j.Slf4j;

import manager.recaps.charts.RenderResult;
import manager.recaps.charts.ResultFromFile;

import models.Person;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.enumerate.CheckType;

import org.joda.time.LocalDate;

import play.jobs.Job;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

/**
 * @author daniele
 * @since 05/07/16.
 */
@Slf4j
public class chartJob extends Job<List<RenderResult>> {

  private Person person;
  private List<ResultFromFile> list;

  @Inject
  static AbsenceDao absenceDao;

  public chartJob(Person person, List<ResultFromFile> list) {
    super();
    this.person = person;
    this.list = list;
  }

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
          .filter(r -> r.personDay.date.isEqual(item.dataAssenza))
          .collect(Collectors.toList());
      if (!values.isEmpty()) {
        final Predicate<Absence> compareCode = a -> a.absenceType.code.equalsIgnoreCase(item.codice)
            || a.absenceType.certificateCode.equalsIgnoreCase(item.codice);
        if (values.stream().anyMatch(compareCode)) {
          result = new RenderResult(null, person.number, person.name,
              person.surname, item.codice, item.dataAssenza, true, "Ok",
              values.stream().filter(compareCode)
                  .findFirst().get().absenceType.code, CheckType.SUCCESS);
        } else {
          result = new RenderResult(null, person.number, person.name,
              person.surname, item.codice, item.dataAssenza, false,
              "Mismatch tra assenza trovata e quella dello schedone",
              values.stream().findFirst().get().absenceType.code, CheckType.WARNING);
        }
      } else {
        result = new RenderResult(null, person.number, person.name,
            person.surname, item.codice, item.dataAssenza, false,
            "Nessuna assenza per il giorno", null, CheckType.DANGER);
      }
      resultList.add(result);
    });

    return resultList;
  }
}
