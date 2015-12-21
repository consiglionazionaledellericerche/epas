package manager.services.vacations.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import dao.AbsenceDao;
import dao.AbsenceTypeDao;
import dao.ContractDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import manager.ConfYearManager;
import manager.cache.AbsenceTypeManager;
import manager.services.vacations.IVacationsRecap;
import manager.services.vacations.IVacationsService;

import models.Absence;
import models.AbsenceType;
import models.Contract;
import models.Office;
import models.Person;
import models.enumerate.AbsenceTypeMapping;
import models.enumerate.Parameter;

import org.joda.time.LocalDate;

import java.util.List;

import javax.inject.Inject;

/**
 * Implementazione di produzione del servizio ferie e permessi.
 * @author alessandro
 *
 */
public class RealVacationsService implements IVacationsService {

  private final AbsenceDao absenceDao;
  private final ContractDao contractDao;
  private final AbsenceTypeDao absenceTypeDao;
  
  private final AbsenceTypeManager absenceTypeManager;
  private final ConfYearManager confYearManager;
  private final IWrapperFactory wrapperFactory;

  @Inject
  RealVacationsService(
      AbsenceDao absenceDao, 
      AbsenceTypeDao absenceTypeDao, 
      ContractDao contractDao,
      AbsenceTypeManager absenceTypeManager,
      ConfYearManager confYearManager,
      IWrapperFactory wrapperFactory) {
    
    this.absenceDao = absenceDao;
    this.absenceTypeDao = absenceTypeDao;
    this.contractDao = contractDao;
    this.absenceTypeManager = absenceTypeManager;
    this.confYearManager = confYearManager;
    this.wrapperFactory = wrapperFactory;
  }

  /**
   * Costruttore del recap.
   */
  @Override
  public IVacationsRecap build(int year, Contract contract, List<Absence> absencesToConsider,
      LocalDate accruedDate, LocalDate dateExpireLastYear, boolean considerDateExpireLastYear,
      Optional<LocalDate> dateAsToday) {
    
    return new VacationsRecapImpl(year, contract, absencesToConsider, LocalDate.now(), 
        dateExpireLastYear, true, dateAsToday); 
  }
  
  /**
   * Costruisce il riepilogo ferie.
   * @param year anno
   * @param contract contratto
   * @param actualDate data di maturazione
   * @param considerExpireLastYear se considerare la scadenza delle ferie nell'anno.
   * @param otherAbsences altre assenze extra db.
   * @param dateAsToday per simulare oggi con un giorno diverso da oggi
   * @return il recap
   */
  @Override
  public Optional<IVacationsRecap> create(int year, Contract contract,
      LocalDate actualDate, boolean considerExpireLastYear,
      List<Absence> otherAbsences, Optional<LocalDate> dateAsToday) {

    IWrapperContract wrContract = wrapperFactory.create(contract);

    if (contract == null || actualDate == null) {
      return Optional.<IVacationsRecap>absent();
    }

    if (wrContract.getValue().vacationPeriods == null 
        || wrContract.getValue().vacationPeriods.isEmpty()) {
      return Optional.<IVacationsRecap>absent();
    }

    // Controllo della dipendenza con i riepiloghi
    if (!wrContract.hasMonthRecapForVacationsRecap(year)) {
      return Optional.<IVacationsRecap>absent();
    }

    if (actualDate.getYear() > year) {
      // FIXME: deve essere il chiamante a non passare la data di oggi
      // e qui la inizializzo in modo appropriato.
      actualDate = new LocalDate(year, 12, 31);
    }

    LocalDate dateExpireLastYear = vacationsLastYearExpireDate(year, contract.person.office);
    
    List<Absence> absencesToConsider = absenceToConsider(wrContract.getValue().person, year, 
        wrContract.getContractDatabaseInterval(), dateAsToday);
    absencesToConsider.addAll(otherAbsences);

    IVacationsRecap vacationRecap = build(year, contract, absencesToConsider, actualDate, 
        dateExpireLastYear, considerExpireLastYear, dateAsToday);

    return Optional.fromNullable(vacationRecap);
  }

  /**
   * Costruisce il riepilogo ferie con valori di default. 
   * @param year anno
   * @param contract contratto
   * @param actualDate data maturazione
   * @param considerExpireLastYear se considerare la scadenza delle ferie nell'anno.
   * @return il recap
   */
  @Override
  public Optional<IVacationsRecap> create(int year, Contract contract, LocalDate actualDate,
      boolean considerExpireLastYear) {

    List<Absence> otherAbsences = Lists.newArrayList();
    
    return create(year, contract, actualDate, considerExpireLastYear,
            otherAbsences, Optional.<LocalDate>absent());
  }
  
  /**
   * Costruisce il riepilogo ferie con valori di default. 
   * @param year anno
   * @param contract contratto
   * @param actualDate data di maturazione
   * @param considerExpireLastYear se considerare la scadenza delle ferie nell'anno.
   * @param dateAsToday per simulare oggi con un giorno diverso da oggi
   * @return il recap
   */
  @Override
  public Optional<IVacationsRecap> create(int year, Contract contract, LocalDate actualDate, 
      boolean considerExpireLastYear, LocalDate dateAsToday) {

    List<Absence> otherAbsences = Lists.newArrayList();
    return create(year, contract, actualDate, considerExpireLastYear,
            otherAbsences, Optional.fromNullable(dateAsToday));
  }

  
  private List<Absence> absenceToConsider(Person person, int year, 
      DateInterval contractDatabaseInterval, Optional<LocalDate> dateAsToday) {

    // Gli intervalli su cui predere le assenze nel db
    DateInterval previousYearInterval = DateUtility
            .intervalIntersection(contractDatabaseInterval,
                    new DateInterval(new LocalDate(year - 1, 1, 1),
                            new LocalDate(year - 1, 12, 31)));
    DateInterval requestYearInterval = DateUtility
            .intervalIntersection(contractDatabaseInterval,
                    new DateInterval(new LocalDate(year, 1, 1),
                            new LocalDate(year, 12, 31)));
    DateInterval nextYearInterval = DateUtility
            .intervalIntersection(contractDatabaseInterval,
                    new DateInterval(new LocalDate(year + 1, 1, 1),
                            new LocalDate(year + 1, 12, 31)));

    // Il contratto deve essere attivo nell'anno...
    Preconditions.checkNotNull(requestYearInterval);
    LocalDate dateFrom = requestYearInterval.getBegin();
    LocalDate dateTo = requestYearInterval.getEnd();
    if (previousYearInterval != null) {
      dateFrom = previousYearInterval.getBegin();
    }
    if (nextYearInterval != null) {
      dateTo = nextYearInterval.getEnd();
    }

    // Le assenze
    List<Absence> absencesForVacationsRecap = absenceDao
            .getAbsencesInCodeList(person, dateFrom, dateTo,
                    absenceTypeManager.codesForVacations(), true);

    return absencesForVacationsRecap;
  }
  
  /**
   * Il primo codice utilizzabile nella data. Ordine: 31, 32, 94.
   * @param person persona
   * @param date data 
   * @param otherAbsences altre assenze da considerare.
   * @return tipo assenza
   */
  public AbsenceType whichVacationCode(Person person, LocalDate date, 
      List<Absence> otherAbsences) {

    Contract contract = contractDao.getContract(date, person);
    Optional<IVacationsRecap> vr = create(date.getYear(),
            contract, date, true, otherAbsences, Optional.<LocalDate>absent());
    
    if (!vr.isPresent()) {
      return null;
    }

    if (vr.get().getVacationDaysLastYearNotYetUsed() > 0) {
      return absenceTypeDao.getAbsenceTypeByCode(
              AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE.getCode()).get();
    }

    if (vr.get().getPersmissionNotYetUsed() > 0)  {

      return absenceTypeDao.getAbsenceTypeByCode(
              AbsenceTypeMapping.FESTIVITA_SOPPRESSE.getCode()).get();
    }

    if (vr.get().getVacationDaysCurrentYearNotYetUsed() > 0) {
      return absenceTypeDao.getAbsenceTypeByCode(
              AbsenceTypeMapping.FERIE_ANNO_CORRENTE.getCode()).get();
    }

    return null;
  }

  /**
   * Verifica che la persona alla data possa prendere un giorno di ferie codice 32.
   * @param person persona
   * @param date data 
   * @param otherAbsences altre assenze da considerare.
   * @return true se è possibile prendere il codice 32.
   */
  public boolean canTake32(Person person, LocalDate date, List<Absence> otherAbsences) {

    Contract contract = contractDao.getContract(date, person);
    Optional<IVacationsRecap> vr = create(date.getYear(),
            contract, date, true, otherAbsences, Optional.<LocalDate>absent());
    if (!vr.isPresent()) {
      return false;
    }

    return (vr.get().getVacationDaysCurrentYearNotYetUsed() > 0);

  }

  /**
   * Verifica che la persona alla data possa prendere un giorno di ferie codice 31.
   * @param person persona
   * @param date data 
   * @param otherAbsences altre assenze da considerare.
   * @return true se è possibile prendere il codice 31.
   */
  public boolean canTake31(Person person, LocalDate date, List<Absence> otherAbsences) {

    Contract contract = contractDao.getContract(date, person);
    Optional<IVacationsRecap> vr = create(date.getYear(),
            contract, date, true, otherAbsences, Optional.<LocalDate>absent());
    if (!vr.isPresent()) {
      return false;
    }


    return (vr.get().getVacationDaysLastYearNotYetUsed() > 0);
  }

  /**
   * Verifica che la persona alla data possa prendere un giorno di permesso con codice 94.
   * @param person persona
   * @param date data 
   * @param otherAbsences altre assenze da considerare.
   * @return true se è possibile prendere il codice 94.
   */
  public boolean canTake94(Person person, LocalDate date, List<Absence> otherAbsences) {

    Contract contract = contractDao.getContract(date, person);
    Optional<IVacationsRecap> vr = create(date.getYear(),
            contract, date, true, otherAbsences, Optional.<LocalDate>absent());
    if (!vr.isPresent()) {
      return false;
    }

    return (vr.get().getPersmissionNotYetUsed() > 0);

  }

  /**
   * La data di scadenza delle ferie anno passato per l'office passato come argomento nell'anno.
   * year.
   * @param year anno
   * @param office office
   * @return data expire
   */
  public LocalDate vacationsLastYearExpireDate(int year, Office office) {

    Integer monthExpiryVacationPastYear = confYearManager.getIntegerFieldValue(
        Parameter.MONTH_EXPIRY_VACATION_PAST_YEAR, office, year);

    Integer dayExpiryVacationPastYear = confYearManager.getIntegerFieldValue(
        Parameter.DAY_EXPIRY_VACATION_PAST_YEAR, office, year);

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
