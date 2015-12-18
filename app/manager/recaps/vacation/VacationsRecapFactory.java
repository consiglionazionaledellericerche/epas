package manager.recaps.vacation;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import dao.AbsenceDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;

import manager.ConfYearManager;
import manager.cache.AbsenceTypeManager;
import manager.vacations.VacationManager;

import models.Absence;
import models.Contract;

import org.joda.time.LocalDate;

import java.util.List;

import javax.inject.Inject;

public class VacationsRecapFactory {

  private final AbsenceDao absenceDao;
  private final AbsenceTypeManager absenceTypeManager;
  private final VacationManager vacationManager;
  private final IWrapperFactory wrapperFactory;

  @Inject
  VacationsRecapFactory(IWrapperFactory wrapperFactory, AbsenceDao absenceDao,
                        AbsenceTypeManager absenceTypeManager,
                        VacationManager vacationManager) {
    this.wrapperFactory = wrapperFactory;
    this.absenceDao = absenceDao;
    this.absenceTypeManager = absenceTypeManager;
    this.vacationManager = vacationManager;
  }

  /**
   * Costruisce il vacationRecap.
   * @param year
   * @param contract
   * @param actualDate
   * @param considerExpireLastYear
   * @param otherAbsences
   * @param dateAsToday per simulare oggi con un giorno diverso da oggi
   * @return il recap
   */
  public Optional<VacationsRecap> create(int year, Contract contract,
      LocalDate actualDate, boolean considerExpireLastYear,
      List<Absence> otherAbsences, Optional<LocalDate> dateAsToday) {

    IWrapperContract wrContract = wrapperFactory.create(contract);

    if (contract == null || actualDate == null) {
      return Optional.<VacationsRecap>absent();
    }

    if (wrContract.getValue().vacationPeriods == null 
        || wrContract.getValue().vacationPeriods.isEmpty()) {
      return Optional.<VacationsRecap>absent();
    }

    // Controllo della dipendenza con i riepiloghi
    if (!wrContract.hasMonthRecapForVacationsRecap(year)) {
      return Optional.<VacationsRecap>absent();
    }

    if (actualDate.getYear() > year) {
      // FIXME: deve essere il chiamante a non passare la data di oggi
      // e qui la inizializzo in modo appropriato.
      actualDate = new LocalDate(year, 12, 31);
    }

    LocalDate dateExpireLastYear = 
        vacationManager.vacationsLastYearExpireDate(year, contract.person.office);
    
    VacationsRecap vacationRecap = new VacationsRecap(absenceDao, absenceTypeManager, year, 
        wrContract, Optional.fromNullable(actualDate), dateExpireLastYear,
            considerExpireLastYear, otherAbsences, dateAsToday);

    return Optional.fromNullable(vacationRecap);
  }

  /**
   *
   * @param person
   * @param month
   * @param year
   * @return
   */
  public Optional<VacationsRecap> create(int year, Contract contract,
                                         LocalDate actualDate, boolean considerExpireLastYear) {

    List<Absence> otherAbsences = Lists.newArrayList();
    return create(year, contract, actualDate, considerExpireLastYear,
            otherAbsences, Optional.<LocalDate>absent());
  }

  /**
   *
   * @param person
   * @param month
   * @param year
   * @return
   */
  public Optional<VacationsRecap> create(int year, Contract contract,
                                         LocalDate actualDate, boolean considerExpireLastYear,
                                         LocalDate dateAsToday) {

    List<Absence> otherAbsences = Lists.newArrayList();
    return create(year, contract, actualDate, considerExpireLastYear,
            otherAbsences, Optional.fromNullable(dateAsToday));
  }

}
