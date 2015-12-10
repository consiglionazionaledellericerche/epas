package manager;

import java.util.List;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.google.inject.Inject;

import dao.AbsenceDao;
import dao.AbsenceTypeDao;
import dao.wrapper.IWrapperContract;
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import models.Absence;
import models.Office;
import models.VacationPeriod;
import models.enumerate.Parameter;

public class VacationManager {

  private final ConfYearManager confYearManager;

  @Inject
  public VacationManager(AbsenceDao absenceDao, AbsenceTypeDao absenceTypeDao,
                         ConfYearManager confYearManager) {
    this.confYearManager = confYearManager;
  }

  /**
   * @return il numero di giorni di permesso legge spettanti al dipendente a seconda dei giorni di
   * presenza
   */
  public static int convertWorkDaysToPermissionDays(int days) {
    int permissionDays = 0;
    if (days >= 45 && days <= 135)
      permissionDays = 1;
    if (days >= 136 && days <= 225)
      permissionDays = 2;
    if (days >= 226 && days <= 315)
      permissionDays = 3;
    if (days >= 316 && days <= 365)
      permissionDays = 4;
    return permissionDays;
  }

  /**
   * La data di scadenza delle ferie anno passato per l'office passato come argomento, nell'anno
   * year.
   */
  public LocalDate vacationsLastYearExpireDate(int year, Office office) {

    Integer monthExpiryVacationPastYear = confYearManager.getIntegerFieldValue(Parameter.MONTH_EXPIRY_VACATION_PAST_YEAR, office, year);

    Integer dayExpiryVacationPastYear = confYearManager.getIntegerFieldValue(Parameter.DAY_EXPIRY_VACATION_PAST_YEAR, office, year);

    LocalDate expireDate = LocalDate.now()
        .withYear(year)
        .withMonthOfYear(monthExpiryVacationPastYear)
        .withDayOfMonth(dayExpiryVacationPastYear);
    return expireDate;
  }

  

  /**
   * @param year       l'anno per il quale vogliamo capire se le ferie dell'anno precedente sono
   *                   scadute
   * @param expireDate l'ultimo giorno utile per usufruire delle ferie dell'anno precedente
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

  /**
   * @return numero di permessi maturati nel periodo yearInterval associati a contract
   */
  public int getPermissionAccruedYear(IWrapperContract wrContract, int year, Optional<LocalDate> accruedDate) {

    //Calcolo l'intersezione fra l'anno e il contratto attuale
    DateInterval yearInterval = new DateInterval(new LocalDate(year, 1, 1),
            new LocalDate(year, 12, 31));
    if (accruedDate.isPresent()) {
      yearInterval = new DateInterval(new LocalDate(year, 1, 1),
              accruedDate.get());
    }
    yearInterval = DateUtility.intervalIntersection(yearInterval,
            wrContract.getContractDateInterval());

    if (yearInterval == null) {
      return 0;
    }

    //int days = 0;
    int permissionDays = 0;

    for (VacationPeriod vp : wrContract.getValue().vacationPeriods) {
      int days = 0;
      DateInterval vpInterval = new DateInterval(vp.beginFrom, vp.endTo);
      DateInterval intersection =
              DateUtility.intervalIntersection(vpInterval, yearInterval);

      if (intersection != null) {
        days = DateUtility.daysInInterval(intersection);
      }
      if (vp.vacationCode.equals("21+3") || vp.vacationCode.description.equals("22+3")) {
        permissionDays = permissionDays + convertWorkDaysToPermissionDaysPartTime(days);

      } else {
        permissionDays = permissionDays + convertWorkDaysToPermissionDays(days);
      }
    }

    return permissionDays;
  }

  /**
   * @return il numero di giorni di ferie maturati nell'anno year calcolati a partire dai piani
   * ferie associati al contratto corrente
   */
  public int getVacationAccruedYear(IWrapperContract wrContract, int year,
                                    Optional<LocalDate> accruedDate, List<Absence> postPartum) {

    LocalDate beginYear = new LocalDate(year, 1, 1);
    LocalDate endYear = new LocalDate(year, 12, 31);

    //Calcolo l'intersezione fra l'anno e il contratto attuale
    DateInterval yearInterval = new DateInterval(beginYear, endYear);

    if (accruedDate.isPresent()) {
      yearInterval = new DateInterval(new LocalDate(year, 1, 1),
              accruedDate.get());
    }
    yearInterval = DateUtility.intervalIntersection(yearInterval,
            wrContract.getContractDateInterval());

    if (yearInterval == null) {
      return 0;
    }

    //per ogni piano ferie conto i giorni trascorsi in yearInterval
    //e applico la funzione di conversione
    int vacationDays = 0;

    //Variabili di supporto
    int totalYearPostPartum = 0;
    int minVacationPeriod = 28;

    for (VacationPeriod vp : wrContract.getValue().vacationPeriods) {

      int days = 0;

      DateInterval vpInterval = new DateInterval(vp.beginFrom, vp.endTo);
      DateInterval intersection =
              DateUtility.intervalIntersection(vpInterval, yearInterval);

      if (intersection != null) {

        // il piano ferie col minor numero di ferie è un potenziale limite
        // inferiore (issue tarveniti)
        if (vp.vacationCode.vacationDays < minVacationPeriod) {
          minVacationPeriod = vp.vacationCode.vacationDays;
        }

        int postPartumInIntersection = filterAbsences(postPartum, intersection);
        days = DateUtility.daysInInterval(intersection)
                - postPartumInIntersection;

        totalYearPostPartum += postPartumInIntersection;

        //calcolo i giorni maturati col metodo di conversione

        if (vp.vacationCode.description.equals("26+4")) {
          vacationDays = vacationDays + convertWorkDaysToVacationDaysLessThreeYears(days);
        }
        if (vp.vacationCode.description.equals("28+4")) {
          vacationDays = vacationDays + convertWorkDaysToVacationDaysMoreThreeYears(days);
        }
        if (vp.vacationCode.description.equals("21+3")) {
          vacationDays = vacationDays + converWorkDaysToVacationDaysPartTime(days);
        }
        if (vp.vacationCode.description.equals("22+3")) {
          vacationDays = vacationDays + converWorkDaysToVacationDaysPartTimeMoreThanThreeYears(
                  days);
        }
      }
    }

    // FIXME: decidere se deve essere un parametro di configurazione
    if (vacationDays > 28) {
      vacationDays = 28;
    }

    // (issue tarveniti)
    // passare da 26 a 28 ed avere 25... aggiusto il calcolo se
    // il contratto copre tutto l'anno richiesto e se non ho avuto assenze
    // postPartum che abbassano i giorno per ferie maturate.
    if (totalYearPostPartum == 0 && yearInterval.getBegin().equals(beginYear)
            && yearInterval.getEnd().equals(endYear)) {
      if (DateUtility.isIntervalIntoAnother(yearInterval, wrContract
              .getContractDateInterval()) && minVacationPeriod > vacationDays) {
        vacationDays = minVacationPeriod;
      }
    }

    return vacationDays;

  }

  /**
   *
   * @param absences
   * @param interval
   * @return
   */
  private int filterAbsences(List<Absence> absences, DateInterval interval) {
    int count = 0;
    for (Absence ab : absences) {
      if (DateUtility.isDateIntoInterval(ab.personDay.date, interval)) {
        count++;
      }
    }
    return count;
  }

  /**
   * @return il numero di giorni di ferie che corrispondono al numero di giorni lavorati dall'inizio
   * dell'anno per chi lavora in istituto da meno di tre anni
   */
  public int convertWorkDaysToVacationDaysLessThreeYears(int days) {

    if (days <= 0)
      return 0;

    if (days >= 1 && days <= 15)
      return 0;
    if (days >= 16 && days <= 45)
      return 2;
    if (days >= 46 && days <= 75)
      return 4;
    if (days >= 76 && days <= 106)
      return 6;
    if (days >= 107 && days <= 136)
      return 8;
    if (days >= 137 && days <= 167)
      return 10;
    if (days >= 168 && days <= 197)
      return 13;
    if (days >= 198 && days <= 227)
      return 15;
    if (days >= 228 && days <= 258)
      return 17;
    if (days >= 259 && days <= 288)
      return 19;
    if (days >= 289 && days <= 319)
      return 21;
    if (days >= 320 && days <= 349)
      return 23;

    else
      return 26;

  }

  /**
   * @return il numero di giorni di ferie che corrispondono al numero di giorni lavorati dall'inizio
   * dell'anno per chi lavora in istituto da più di tre anni
   */
  public int convertWorkDaysToVacationDaysMoreThreeYears(int days) {
    if (days <= 0)
      return 0;

    if (days >= 1 && days <= 15)
      return 0;
    if (days >= 16 && days <= 45)
      return 2;
    if (days >= 46 && days <= 75)
      return 4;
    if (days >= 76 && days <= 106)
      return 7;
    if (days >= 107 && days <= 136)
      return 9;
    if (days >= 137 && days <= 167)
      return 11;
    if (days >= 168 && days <= 197)
      return 14;
    if (days >= 198 && days <= 227)
      return 16;
    if (days >= 228 && days <= 258)
      return 18;
    if (days >= 259 && days <= 288)
      return 21;
    if (days >= 289 && days <= 319)
      return 23;
    if (days >= 320 && days <= 349)
      return 25;
    else
      return 28;

  }

  /**
   * @return il numero di giorni di ferie maturati secondo il piano di accumulo previsto per il part
   * time verticale
   */
  public int converWorkDaysToVacationDaysPartTime(int days) {
    if (days <= 0)
      return 0;

    if (days >= 1 && days <= 15)
      return 0;
    if (days >= 16 && days <= 45)
      return 2;
    if (days >= 46 && days <= 75)
      return 3;
    if (days >= 76 && days <= 106)
      return 5;
    if (days >= 107 && days <= 136)
      return 6;
    if (days >= 137 && days <= 167)
      return 8;
    if (days >= 168 && days <= 197)
      return 10;
    if (days >= 198 && days <= 227)
      return 12;
    if (days >= 228 && days <= 258)
      return 14;
    if (days >= 259 && days <= 288)
      return 15;
    if (days >= 289 && days <= 319)
      return 17;
    if (days >= 320 && days <= 349)
      return 18;
    else
      return 21;
  }

  public int converWorkDaysToVacationDaysPartTimeMoreThanThreeYears(int days) {
    if (days <= 0)
      return 0;
    if (days >= 1 && days <= 15)
      return 0;
    if (days >= 16 && days <= 45)
      return 2;
    if (days >= 46 && days <= 75)
      return 3;
    if (days >= 76 && days <= 106)
      return 6;
    if (days >= 107 && days <= 136)
      return 7;
    if (days >= 137 && days <= 167)
      return 9;
    if (days >= 168 && days <= 197)
      return 11;
    if (days >= 198 && days <= 227)
      return 13;
    if (days >= 228 && days <= 258)
      return 14;
    if (days >= 259 && days <= 288)
      return 17;
    if (days >= 289 && days <= 319)
      return 18;
    if (days >= 320 && days <= 349)
      return 20;
    else
      return 22;
  }

  /**
   * @return il numero di giorni di permesso maturati con il piano ferie relativo al part time
   */
  public int convertWorkDaysToPermissionDaysPartTime(int days) {
    int permissionDays = 0;
    if (days >= 45 && days <= 135)
      permissionDays = 1;
    if (days >= 136 && days <= 315)
      permissionDays = 2;
    if (days >= 316 && days <= 365)
      permissionDays = 3;
    return permissionDays;
  }

}
