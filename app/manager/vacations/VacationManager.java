package manager.vacations;

import com.google.inject.Inject;

import dao.AbsenceDao;
import dao.AbsenceTypeDao;

import manager.ConfYearManager;

import models.Office;
import models.enumerate.Parameter;

import org.joda.time.LocalDate;

public class VacationManager {

  private final ConfYearManager confYearManager;

  @Inject
  public VacationManager(AbsenceDao absenceDao, AbsenceTypeDao absenceTypeDao,
                         ConfYearManager confYearManager) {
    
    this.confYearManager = confYearManager;
  }

  /**
   * La data di scadenza delle ferie anno passato per l'office passato come argomento nell'anno
   * year.
   * @param year anno
   * @param office office
   * @return data expire
   */
  public LocalDate vacationsLastYearExpireDate(int year, Office office) {

    Integer monthExpiryVacationPastYear = confYearManager
        .getIntegerFieldValue(Parameter.MONTH_EXPIRY_VACATION_PAST_YEAR, office, year);

    Integer dayExpiryVacationPastYear = confYearManager
        .getIntegerFieldValue(Parameter.DAY_EXPIRY_VACATION_PAST_YEAR, office, year);

    LocalDate expireDate = LocalDate.now()
        .withYear(year)
        .withMonthOfYear(monthExpiryVacationPastYear)
        .withDayOfMonth(dayExpiryVacationPastYear);
    return expireDate;
  }

  /**
   * Se sono scadute le ferie per l'anno passato.
   * @param year anno
   * @param expireDate data scadenza
   * @return esito 
   */
  public boolean isVacationsLastYearExpired(int year, LocalDate expireDate) {
    LocalDate today = LocalDate.now();

    if (year < today.getYear()) {        //query anni passati
      return true;
    } else if (year == today.getYear() && today.isAfter(expireDate)) {    //query anno attuale
      return true;
    }
    return false;
  }
  
}
