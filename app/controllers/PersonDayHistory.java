package controllers;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import dao.PersonDayDao;
import dao.history.AbsenceHistoryDao;
import dao.history.HistoryValue;
import dao.history.PersonDayHistoryDao;
import dao.history.StampingHistoryDao;

import models.Absence;
import models.PersonDay;
import models.Stamping;

import play.mvc.Controller;
import play.mvc.With;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

/**
 * Controller per la visualizzazione dello storico dei PersonDay.
 *
 * @author marco
 */
@With({Resecure.class})
public class PersonDayHistory extends Controller {

  @Inject
  static PersonDayHistoryDao personDayHistoryDao;
  @Inject
  static StampingHistoryDao stampingHistoryDao;
  @Inject
  static AbsenceHistoryDao absenceHistoryDao;
  @Inject
  static PersonDayDao personDayDao;


  /**
   * Visualizzazione dello storico dei PersonDay.
   *
   * @param personDayId l'id del personDay di cui mostrare lo storico
   */
  public static void personDayHistory(long personDayId) {

    boolean found = false;
    final PersonDay personDay = PersonDay.findById(personDayId);
    if (personDay == null) {

      render(found);
    }
    found = true;
    List<HistoryValue<Absence>> allAbsences = personDayHistoryDao
        .absences(personDayId);

    Set<Long> absenceIds = Sets.newHashSet();
    for (HistoryValue<Absence> historyValue : allAbsences) {
      absenceIds.add(historyValue.value.id);
    }

    List<Long> sortedAbsencesIds = Lists.newArrayList(absenceIds);
    Collections.sort(sortedAbsencesIds);

    //Lista di absences
    List<List<HistoryValue<Absence>>> historyAbsencesList = Lists.newArrayList();

    for (Long absenceId : sortedAbsencesIds) {

      List<HistoryValue<Absence>> historyAbsence = absenceHistoryDao
          .absences(absenceId);
      historyAbsencesList.add(historyAbsence);
    }

    List<HistoryValue<Stamping>> allStampings = personDayHistoryDao
        .stampings(personDayId);

    Set<Long> stampingIds = Sets.newHashSet();
    for (HistoryValue<Stamping> historyValue : allStampings) {
      stampingIds.add(historyValue.value.id);
    }

    List<Long> sortedStampingsIds = Lists.newArrayList(stampingIds);
    Collections.sort(sortedStampingsIds);

    //Lista di stampings
    List<List<HistoryValue<Stamping>>> historyStampingsList = Lists.newArrayList();

    for (Long stampingId : sortedStampingsIds) {

      List<HistoryValue<Stamping>> historyStamping = stampingHistoryDao
          .stampings(stampingId);
      historyStampingsList.add(historyStamping);
    }
    render(historyStampingsList, historyAbsencesList, personDay, found);
  }
}
