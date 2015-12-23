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
public class VacationsService implements IVacationsService {

  private final AbsenceDao absenceDao;
  private final ContractDao contractDao;
  private final AbsenceTypeDao absenceTypeDao;

  private final AbsenceTypeManager absenceTypeManager;
  private final ConfYearManager confYearManager;
  private final IWrapperFactory wrapperFactory;

  @Inject
  public VacationsService(
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
   * Costruisce il riepilogo ferie.
   * @param year anno
   * @param contract contratto
   * @param accruedDate data di maturazione
   * @param considerExpireLastYear se considerare la scadenza delle ferie nell'anno.
   * @param otherAbsences altre assenze extra db.
   * @param dateAsToday per ignorare tutto ciò che viene dopo.
   * @return il recap
   */
  private Optional<IVacationsRecap> create(int year, Contract contract,
      LocalDate accruedDate, List<Absence> otherAbsences, Optional<LocalDate> dateAsToday) {

    IWrapperContract wrContract = wrapperFactory.create(contract);

    if (contract == null || accruedDate == null) {
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

    Preconditions.checkState(accruedDate.getYear() <= year);
    //    if (accruedDate.getYear() > year) {
    //      Preconditions.checkState(expression);
    //      // FIXME: deve essere il chiamante a non passare la data di oggi
    //      // e qui la inizializzo in modo appropriato.
    //      accruedDate = new LocalDate(year, 12, 31);
    //    }

    LocalDate expireDateLastYear = vacationsLastYearExpireDate(year, contract.person.office);
    LocalDate expireDateCurrentYear = vacationsLastYearExpireDate(year + 1, contract.person.office);

    List<Absence> absencesToConsider = absenceToConsider(wrContract.getValue().person, year,
        wrContract.getContractDatabaseInterval(), dateAsToday);
    absencesToConsider.addAll(otherAbsences);

    IVacationsRecap vacationRecap = VacationsRecap.builder()
        .year(year)
        .contract(contract)
        .absencesToConsider(absencesToConsider)
        .accruedDate(accruedDate)
        .expireDateLastYear(expireDateLastYear)
        .expireDateCurrentYear(expireDateCurrentYear)
        .build();

    return Optional.fromNullable(vacationRecap);
  }

  /**
   * Preleva dal db le assenze da considerare per la costruzione del riepilogo ferie dell'anno year.
   * Se dateAsToday è popolato ignora tutte le assenze fatte (sia ferie/permessi che postPartum)
   * prese dopo tale data.
   */
  public List<Absence> absenceToConsider(Person person, int year,
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

    // limite superiode dateAsToday. Messo qua per non sporcare i calcoli a livelli successivi,
    // visto che serve solo per il tabellone di danila.
    if (dateAsToday.isPresent()) {
      if (DateUtility.isDateIntoInterval(dateAsToday.get(), previousYearInterval)) {
        previousYearInterval = new DateInterval(previousYearInterval.getBegin(), dateAsToday.get());
      }
      if (DateUtility.isDateIntoInterval(dateAsToday.get(), requestYearInterval)) {
        requestYearInterval = new DateInterval(requestYearInterval.getBegin(), dateAsToday.get());
      }
      if (DateUtility.isDateIntoInterval(dateAsToday.get(), nextYearInterval)) {
        nextYearInterval = new DateInterval(nextYearInterval.getBegin(), dateAsToday.get());
      }
    }

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
   * Costruisce il riepilogo ferie con il calcolo di assenze maturate e residue a oggi considerando
   * la data di scadenza ferie anno passato della sede competente.
   * @param year anno
   * @param contract contratto
   * @return il recap
   */
  @Override
  public Optional<IVacationsRecap> create(int year, Contract contract) {

    LocalDate accruedDate = LocalDate.now();

    List<Absence> otherAbsences = Lists.newArrayList();

    return create(year, contract, accruedDate, otherAbsences, Optional.<LocalDate>absent());
  }

  /**
   * Costruisce il riepilogo ferie alla fine del mese con il calcolo delle assenze
   * maturate e residue fino a quel momento (ignorando le eventuali assenze prese successivamente).
   * Serve a Danila.
   * @param year anno
   * @param month anno
   * @param contract contratto
   * @return il recap
   */
  @Override
  public Optional<IVacationsRecap> createEndMonth(int year, int month, Contract contract) {

    LocalDate endMonth = new LocalDate(year, month, 1).dayOfMonth().withMaximumValue();
    List<Absence> otherAbsences = Lists.newArrayList();
    return create(year, contract, endMonth, otherAbsences, Optional.fromNullable(endMonth));
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
    Optional<IVacationsRecap> vr = create(date.getYear(), contract, date, otherAbsences,
        Optional.<LocalDate>absent());

    if (!vr.isPresent()) {
      return null;
    }

    if (vr.get().getVacationsLastYear().getNotYetUsedAccrued() > 0) {
      return absenceTypeDao.getAbsenceTypeByCode(
              AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE.getCode()).get();
    }

    if (vr.get().getPermissions().getNotYetUsedAccrued() > 0)  {

      return absenceTypeDao.getAbsenceTypeByCode(
              AbsenceTypeMapping.FESTIVITA_SOPPRESSE.getCode()).get();
    }

    if (vr.get().getVacationsCurrentYear().getNotYetUsedAccrued() > 0) {
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
    Optional<IVacationsRecap> vr = create(date.getYear(), contract, date, otherAbsences,
        Optional.<LocalDate>absent());
    if (!vr.isPresent()) {
      return false;
    }

    return (vr.get().getVacationsCurrentYear().getNotYetUsedAccrued() > 0);

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
    Optional<IVacationsRecap> vr = create(date.getYear(), contract, date, otherAbsences,
        Optional.<LocalDate>absent());
    if (!vr.isPresent()) {
      return false;
    }


    return (vr.get().getVacationsLastYear().getNotYetUsedAccrued() > 0);
  }

  /**
   * Verifica che la persona alla data possa prendere un giorno di ferie con codice 37.
   * @param person persona
   * @param date data
   * @param otherAbsences altre assenze da considerare.
   * @return true se è possibile prendere il codice 37.
   */
  public boolean canTake37(Person person, LocalDate date, List<Absence> otherAbsences) {

    if (date.getYear() == LocalDate.now().getYear()) {
      Optional<IVacationsRecap> vacationsRecap = create(date.getYear(),
          contractDao.getContract(LocalDate.now(), person), LocalDate.now(), otherAbsences,
          Optional.<LocalDate>absent());

      if (vacationsRecap.isPresent()) {
        int remaining37 = vacationsRecap.get().getVacationsLastYear().getNotYetUsedTotal();
        if (remaining37 > 0) {
          //37 disponibile
          return true;
        }
      }
    }
    return false;
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
    Optional<IVacationsRecap> vr = create(date.getYear(), contract, date, otherAbsences,
        Optional.<LocalDate>absent());
    if (!vr.isPresent()) {
      return false;
    }

    return (vr.get().getPermissions().getNotYetUsedAccrued() > 0);

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
