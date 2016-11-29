package absences;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dao.absences.AbsenceComponentDao;

import models.absences.Absence;

import java.util.List;

public class DependencyMocker {

  /**
   * Istanza mockata per costruzione PeriodChain.
   * @param orderedAbsence orderedAbsence
   * @param allOrderedAbsence allOrderedAbsence
   * @return absenceComponentDao
   */
  public static AbsenceComponentDao absenceComponentDao(
      List<Absence> orderedAbsence, 
      List<Absence> allOrderedAbsence) {

    AbsenceComponentDao absenceComponentDao = mock(AbsenceComponentDao.class);
    when(absenceComponentDao.orderedAbsences(null, null, null, null))
      .thenReturn(orderedAbsence)
      .thenReturn(allOrderedAbsence);

    return absenceComponentDao;
  }

  
}
