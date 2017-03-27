package dao.wrapper;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import models.Contract;
import models.ContractMonthRecap;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

/**
 * Contract con alcune funzionalità aggiuntive.
 *
 * @author marco
 */
public class WrapperContract implements IWrapperContract {

  private final Contract value;
  private final IWrapperFactory wrapperFactory;

  @Inject
  WrapperContract(@Assisted Contract contract,
                  IWrapperFactory wrapperFactory) {
    this.value = contract;
    this.wrapperFactory = wrapperFactory;
  }

  @Override
  public Contract getValue() {
    return value;
  }

  /**
   * True se il contratto è l'ultimo contratto della persona per mese e anno selezionati.
   *
   * @param month mese
   * @param year  anno
   * @return esito.
   */
  @Override
  public boolean isLastInMonth(int month, int year) {

    DateInterval monthInterval = new DateInterval(new LocalDate(year, month, 1),
        new LocalDate(year, month, 1).dayOfMonth().withMaximumValue());

    for (Contract contract : value.person.contracts) {
      if (contract.id.equals(value.id)) {
        continue;
      }
      DateInterval contractInterval = wrapperFactory.create(contract)
          .getContractDateInterval();
      if (DateUtility.intervalIntersection(monthInterval, contractInterval) != null) {
        if (value.beginDate.isBefore(contract.beginDate)) {
          return false;
        }
      }
    }
    return true;
  }

  public boolean isActive() {
    return DateUtility.isDateIntoInterval(LocalDate.now(), getContractDateInterval());
  }

  /**
   * True se il contratto è a tempo determinato.
   *
   * @return esito.
   */
  @Override
  public boolean isDefined() {

    return this.value.endDate != null;
  }

  /**
   * L'intervallo effettivo per il contratto. Se endContract (data terminazione del contratto) è
   * presente sovrascrive contract.endDate.
   *
   * @return l'intervallo.
   */
  @Override
  public DateInterval getContractDateInterval() {
    if (value.endContract != null) {
      return new DateInterval(value.beginDate, value.endContract);
    } else {
      return new DateInterval(value.beginDate, value.endDate);
    }
  }

  /**
   * L'intervallo ePAS per il contratto. Se è presente una data di inizializzazione generale del
   * contratto sovrascrive contract.beginDate.
   *
   * @return l'intervallo.
   */
  @Override
  public DateInterval getContractDatabaseInterval() {

    // TODO: verificare il funzionamento.
    // Assumo che initUse in configurazione sia ininfluente perchè se definita
    // allora automaticamente deve essere definito sourceContract.

    DateInterval contractInterval = getContractDateInterval();
    if (value.sourceDateResidual != null) {
      return new DateInterval(value.sourceDateResidual.plusDays(1),
          contractInterval.getEnd());
    }

    return contractInterval;
  }

  /**
   * L'intervallo ePAS per il contratto dal punto di vista dei buoni pasto. Se è presente una data
   * di inizializzazione buoni pasto del contratto sovrascrive contract.beginDate.
   *
   * @return l'intervallo.
   */
  @Override
  public DateInterval getContractDatabaseIntervalForMealTicket() {

    DateInterval contractDatebaseInterval = getContractDatabaseInterval();
    if (value.sourceDateMealTicket != null) {
      return new DateInterval(value.sourceDateMealTicket.plusDays(1),
          contractDatebaseInterval.getEnd());
    }

    return contractDatebaseInterval;
  }

  /**
   * Il mese del primo riepilogo esistente per il contratto. absent() se non ci sono i dati per
   * costruire il primo riepilogo.
   *
   * @return il mese (absent se non esiste).
   */
  @Override
  public Optional<YearMonth> getFirstMonthToRecap() {

    if (initializationMissing()) {
      return Optional.<YearMonth>absent();
    }
    if (value.sourceDateResidual != null) {
      return Optional.fromNullable((new YearMonth(value.sourceDateResidual)));
    }
    return Optional.fromNullable(new YearMonth(value.beginDate));
  }

  /**
   * Il mese dell'ultimo riepilogo esistente per il contratto (al momento della chiamata).<br> Il
   * mese attuale se il contratto termina dopo di esso. Altrimenti il mese di fine contratto.
   *
   * @return il mese
   */
  @Override
  public YearMonth getLastMonthToRecap() {
    YearMonth currentMonth = new YearMonth(LocalDate.now());
    YearMonth lastMonth = new YearMonth(getContractDateInterval().getEnd());
    if (currentMonth.isAfter(lastMonth)) {
      return lastMonth;
    }
    return currentMonth;
  }
  
  /**
   * Se il contratto è stato inizializzato per la parte residuale nel mese passato come argomento. 
   * @param yearMonth mese
   * @return esito
   */
  @Override
  public boolean residualInitInYearMonth(YearMonth yearMonth) {
    if (value.sourceDateResidual == null) {
      return false;
    }
    return new YearMonth(value.sourceDateResidual).equals(yearMonth);
  }

  /**
   * Il riepilogo mensile attualmente persistito (se esiste).
   *
   * @param yearMonth mese
   * @return il riepilogo (absent se non presente)
   */
  @Override
  public Optional<ContractMonthRecap> getContractMonthRecap(YearMonth yearMonth) {

    for (ContractMonthRecap cmr : value.contractMonthRecaps) {
      if (cmr.year == yearMonth.getYear()
          && cmr.month == yearMonth.getMonthOfYear()) {
        return Optional.fromNullable(cmr);
      }
    }
    return Optional.absent();
  }

  /**
   * Se sono state definite sia la inizializzazione generale che quella buoni pasto e quella dei
   * buoni pasto è precedente a quella generale.
   *
   * @return esito
   */
  @Override
  public boolean mealTicketInitBeforeGeneralInit() {
    if (value.sourceDateResidual != null && value.sourceDateMealTicket != null
        && value.sourceDateResidual.isAfter(value.sourceDateMealTicket)) {
      return true;
    } else {
      return false;
    }
  }


  /**
   * Se il contratto necessita di inizializzazione.
   *
   * @return esito
   */
  @Override
  public boolean initializationMissing() {

    LocalDate dateForInit = dateForInitialization();

    if (value.sourceDateResidual != null) {
      return false;
    }

    return value.beginDate.isBefore(dateForInit);
  }

  /**
   * Se per il contratto c'è almeno un riepilogo necessario non persistito.
   *
   * @return esito.
   */
  @Override
  public boolean monthRecapMissing() {

    Optional<YearMonth> monthToCheck = getFirstMonthToRecap();
    if (!monthToCheck.isPresent()) {
      return true;
    }
    YearMonth nowMonth = YearMonth.now();
    while (!monthToCheck.get().isAfter(nowMonth)) {
      // FIXME: renderlo efficiente, un dao che li ordina.
      if (!getContractMonthRecap(monthToCheck.get()).isPresent()) {
        return true;
      }
      monthToCheck = Optional.fromNullable(monthToCheck.get().plusMonths(1));
    }

    return false;
  }

  /**
   * Se un riepilogo per il mese passato come argomento non è persistito.
   *
   * @param yearMonth mese
   * @return esito.
   */
  @Override
  public boolean monthRecapMissing(YearMonth yearMonth) {
    return !getContractMonthRecap(yearMonth).isPresent();
  }

  /**
   * @return La data più recente tra la creazione del contratto e la creazione della persona.
   */
  @Override
  public LocalDate dateForInitialization() {

    LocalDate officeBegin = value.person.office.getBeginDate();
    LocalDate personCreation = new LocalDate(value.person.beginDate);
    LocalDate candidate = value.getBeginDate();

    if (candidate.isBefore(officeBegin)) {
      candidate = officeBegin;
    }
    if (candidate.isBefore(personCreation)) {
      candidate = personCreation;
    }

    return candidate;
  }
  
  /**
   * @return La data più recente tra la creazione del contratto e la creazione della persona.
   */
  @Override
  public LocalDate dateForMealInitialization() {

    if (initializationMissing()) {
      return null;
    }
    
    if (value.sourceDateResidual != null) {
      return value.sourceDateResidual;
    }
    
    return value.beginDate.minusDays(1);
  }

  /**
   * Se il contratto è finito prima che la sede della persona fosse installata in ePAS.
   *
   * @return esito
   */
  @Override
  public boolean noRelevant() {

    LocalDate officeInstallation = value.person.office.getBeginDate();

    return officeInstallation.isAfter(getContractDateInterval().getEnd());
  }

  /**
   * Se il contratto ha tutti i riepiloghi necessari per calcolare il riepilogo per l'anno passato
   * come parametro.
   *
   * @param yearToRecap anno
   * @return esito
   */
  @Override
  public boolean hasMonthRecapForVacationsRecap(int yearToRecap) {

    // se non ho il contratto inizializzato il riepilogo ferie non esiste
    //o non è veritiero.
    if (initializationMissing()) {
      return false;
    }

    // se il contratto inizia nell'anno non ho bisogno del recap.
    if (value.beginDate.getYear() == yearToRecap) {
      return true;
    }

    // se source date cade nell'anno non ho bisogno del recap.
    if (value.sourceDateResidual != null
        && value.sourceDateResidual.getYear() == yearToRecap) {
      return true;
    }

    // Altrimenti ho bisogno del riepilogo finale dell'anno precedente.
    Optional<ContractMonthRecap> yearMonthToCheck =
        getContractMonthRecap(new YearMonth(yearToRecap - 1, 12));

    if (yearMonthToCheck.isPresent()) {
      return true;
    }

    return false;
  }



}
