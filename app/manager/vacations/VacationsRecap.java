package manager.vacations;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import dao.AbsenceDao;
import dao.wrapper.IWrapperContract;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import manager.cache.AbsenceTypeManager;
import manager.vacations.AccruedDecision.TypeAccrued;

import models.Absence;
import models.AbsenceType;
import models.Contract;
import models.Person;
import models.enumerate.AbsenceTypeMapping;

import org.joda.time.LocalDate;

import java.util.List;

/**
 * @author alessandro Classe da utilizzare per il riepilogo delle informazioni relative al piano
 *         ferie di una persona.
 */
public class VacationsRecap {

  // DAO E MANAGER
  
  private final AbsenceTypeManager absenceTypeManager;
  private final AbsenceDao absenceDao;
  
  // DATI DELLA RICHIESTA
  
  public Person person;
  public Contract contract;
  public int year;
  public IWrapperContract wrContract = null;
  public DateInterval activeContractInterval = null;
  public LocalDate accruedDate;
  
  // DECISIONI
  
  public AccruedDecision decisionsVacationLastYearAccrued;
  public AccruedDecision decisionsVacationCurrentYearAccrued;
  public AccruedDecision decisionsPermissionYearAccrued;

  public AccruedDecision decisionsVacationCurrentYearTotal;
  public AccruedDecision decisionsPermissionYearTotal;

  // TOTALI
  public Integer vacationDaysCurrentYearTotal = 0;
  public Integer permissionCurrentYearTotal = 0;
  public LocalDate dateExpireLastYear;
  
  // USATE
  public int vacationDaysLastYearUsed = 0;
  public int vacationDaysCurrentYearUsed = 0;
  public int permissionUsed = 0;
  
  // MATURATE
  public Integer vacationDaysLastYearAccrued = 0;
  public Integer vacationDaysCurrentYearAccrued = 0;
  public Integer permissionCurrentYearAccrued = 0;
  
  // RIMANENTI
  public Integer vacationDaysLastYearNotYetUsed = 0;
  public Integer vacationDaysCurrentYearNotYetUsed = 0;
  public Integer persmissionNotYetUsed = 0;
  
  /* true se le ferie dell'anno passato sono scadute */
  public boolean isExpireLastYear = false;
  
  /* true se il contratto termina prima della fine dell'anno richiesto */
  public boolean isExpireBeforeEndYear = false;
  
  /* true se il contratto inizia dopo l'inizio dell'anno richiesto */
  public boolean isActiveAfterBeginYear = false;
  
  // SUPPORTO AL CALCOLO
  
  private DateInterval previousYearInterval;
  private DateInterval requestYearInterval;
  private DateInterval nextYearInterval;
  private List<Absence> list32PreviouYear = Lists.newArrayList();
  private List<Absence> list31RequestYear = Lists.newArrayList();
  private List<Absence> list37RequestYear = Lists.newArrayList();
  private List<Absence> list32RequestYear = Lists.newArrayList();
  private List<Absence> list31NextYear = Lists.newArrayList();
  private List<Absence> list37NextYear = Lists.newArrayList();
  private List<Absence> list94RequestYear = Lists.newArrayList();
  private List<Absence> postPartum = Lists.newArrayList();

  /**
   * Il riepilogo annuale per l'anno e il contratto.
   * 
   * @param absenceDao injected
   * @param absenceTypeManager injected
   * @param year l'anno in considerazione
   * @param wrContract contratto del riepilogo
   * @param accruedDate la data specifica in cui si desidera fotografare la situazione
   * @param dateExpireLastYear la data di scadenza ferie per l'istituto del contratto.
   * @param considerExpireLastYear se si vuole considerare la scadenza ferie in configurazione
   * @param otherAbsences assenze extra-db
   * @param dateAsToday la data di simulazione come se fosse la data di oggi.
   */
  public VacationsRecap(AbsenceDao absenceDao, AbsenceTypeManager absenceTypeManager,
                        int year, IWrapperContract wrContract, Optional<LocalDate> accruedDate,
                        LocalDate dateExpireLastYear, boolean considerExpireLastYear, 
                        List<Absence> otherAbsences, Optional<LocalDate> dateAsToday) {

    this.absenceDao = absenceDao;
    this.absenceTypeManager = absenceTypeManager;

    if (accruedDate.isPresent()) {
      //Preconditions.checkArgument(year == accruedDate.get().getYear());
    } else {
      accruedDate = Optional.fromNullable(LocalDate.now());
    }
    this.accruedDate = accruedDate.get();
    this.wrContract = wrContract;
    this.contract = wrContract.getValue();
    this.person = wrContract.getValue().person;
    this.year = year;
    this.dateExpireLastYear = dateExpireLastYear;
    this.activeContractInterval = wrContract.getContractDateInterval();

    initDataStructures(otherAbsences, dateAsToday);

    //(1) ferie fatte dell'anno precedente all'anno richiesto
    vacationDaysLastYearUsed = list32PreviouYear.size()
            + list31RequestYear.size() + list37RequestYear.size();

    //(2) ferie fatte dell'anno richiesto
    vacationDaysCurrentYearUsed = list32RequestYear.size()
            + list31NextYear.size() + list37NextYear.size();

    //(3) permessi usati dell'anno richiesto
    permissionUsed = list94RequestYear.size();

    if (this.contract.sourceDateResidual != null 
        && this.contract.sourceDateResidual.getYear() == year) {
      vacationDaysLastYearUsed += this.contract.sourceVacationLastYearUsed;
      vacationDaysCurrentYearUsed += this.contract.sourceVacationCurrentYearUsed;
      permissionUsed += this.contract.sourcePermissionUsed;
    }

    //(4) Calcolo ferie e permessi maturati per l'anno passato e l'anno corrente
    // (sono indipendenti dal database)

    AccruedComponent accruedComponent = AccruedComponent.builder()
        .year(year)
        .contractDateInterval(wrContract.getContractDateInterval())
        .accruedDate(accruedDate)
        .contractVacationPeriod(contract.vacationPeriods)
        .postPartum(postPartum).build();

    this.decisionsVacationLastYearAccrued = AccruedDecision.builder()
        .accruedComponent(accruedComponent)
        .typeAccrued(TypeAccrued.VACATION_LAST_YEAR_TOTAL)
        .build();
    
    this.decisionsPermissionYearAccrued = AccruedDecision.builder()
        .accruedComponent(accruedComponent)
        .typeAccrued(TypeAccrued.PERMISSION_CURRENT_YEAR_ACCRUED)
        .build();
    
    this.decisionsVacationCurrentYearAccrued = AccruedDecision.builder()
        .accruedComponent(accruedComponent)
        .typeAccrued(TypeAccrued.VACATION_CURRENT_YEAR_ACCRUED)
        .build();
    
    this.vacationDaysLastYearAccrued = this.decisionsVacationLastYearAccrued.getTotalAccrued();
    this.vacationDaysCurrentYearAccrued = 
        this.decisionsVacationCurrentYearAccrued.getTotalAccrued();
    this.permissionCurrentYearAccrued = this.decisionsPermissionYearAccrued.getTotalAccrued();

    
    //(5) Calcolo ferie e permessi totali per l'anno corrente
    this.decisionsPermissionYearTotal = AccruedDecision.builder()
        .accruedComponent(accruedComponent)
        .typeAccrued(TypeAccrued.PERMISSION_CURRENT_YEAR_TOTAL)
        .build();
            
    this.decisionsVacationCurrentYearTotal = AccruedDecision.builder()
        .accruedComponent(accruedComponent)
        .typeAccrued(TypeAccrued.VACATION_CURRENT_YEAR_TOTAL)
        .build();
    
    this.vacationDaysCurrentYearTotal = this.decisionsVacationCurrentYearTotal.getTotalAccrued();
    this.permissionCurrentYearTotal = this.decisionsPermissionYearTotal.getTotalAccrued();
    
    //(6) Calcolo ferie e permessi non ancora utilizzati 
    //    per l'anno corrente e per l'anno precedente
    //    (sono funzione di quanto calcolato precedentemente)

    //Anno passato
    if (!this.accruedDate.isAfter(dateExpireLastYear) || !considerExpireLastYear) {
      this.vacationDaysLastYearNotYetUsed = this.vacationDaysLastYearAccrued
              - this.vacationDaysLastYearUsed;
    } else {
      this.vacationDaysLastYearNotYetUsed = 0;
    }

    //Anno corrente
    if (this.wrContract.isDefined()) {
      //per i detereminati considero le maturate
      //(perchè potrebbero decidere di cambiare contratto)
      this.vacationDaysCurrentYearNotYetUsed = this.vacationDaysCurrentYearAccrued
              - this.vacationDaysCurrentYearUsed;
      this.persmissionNotYetUsed = this.permissionCurrentYearAccrued
              - this.permissionUsed;

    } else {
      //per gli indeterminati le considero tutte (è più improbabile....)
      this.vacationDaysCurrentYearNotYetUsed = this.vacationDaysCurrentYearTotal
              - this.vacationDaysCurrentYearUsed;
      this.persmissionNotYetUsed = this.permissionCurrentYearTotal
              - this.permissionUsed;
    }

  }

  /**
   * Lista delle ferie usate nell'anno corrente.
   * @return lista assenze
   */
  public List<Absence> listVacationCurrentYearUsed() {
    
    List<Absence> absences = Lists.newArrayList();
    absences.addAll(list32RequestYear);
    absences.addAll(list31NextYear);
    absences.addAll(list37NextYear);
    if (this.contract.sourceDateResidual != null 
        && this.contract.sourceDateResidual.getYear() == year) {
      vacationDaysLastYearUsed += this.contract.sourceVacationLastYearUsed;
      vacationDaysCurrentYearUsed += this.contract.sourceVacationCurrentYearUsed;
      permissionUsed += this.contract.sourcePermissionUsed;
    }

    return absences;
  }

  /**
   * Lista delle ferie usate nell'anno passato.
   * @return lista assenze
   */
  public List<Absence> listVacationLastYearUsed() {
    
    List<Absence> absences = Lists.newArrayList();
    absences.addAll(list32PreviouYear);
    absences.addAll(list31RequestYear);
    absences.addAll(list37RequestYear);
    return absences;
  }

  /**
   * Lista dei permessi usate nell'anno corrente.
   * @return lista assenze
   */
  public List<Absence> listPermissionUsed() {
    
    return list94RequestYear;
  }

  /**
   * Ferie usate anno corrente da inizializzazione.
   * @return numero assenze.
   */
  public int sourceVacationCurrentYearUsed() {
    
    if (this.contract.sourceDateResidual != null 
        && this.contract.sourceDateResidual.getYear() == year) {
      return this.contract.sourceVacationCurrentYearUsed;
    }
    return 0;
  }
  
  /**
   * Ferie usate anno passato da inizializzazione.
   * @return numero assenze.
   */
  public int sourceVacationLastYearUsed() {
    
    if (this.contract.sourceDateResidual != null 
        && this.contract.sourceDateResidual.getYear() == year) {
      return this.contract.sourceVacationLastYearUsed;
    }
    return 0;
  }

  /**
   * Permessi usati anno corrente da inizializzazione.
   * @return numero assenze.
   */
  public int sourcePermissionUsed() {
    
    if (this.contract.sourceDateResidual != null 
        && this.contract.sourceDateResidual.getYear() == year) {
      return this.contract.sourcePermissionUsed;
    }
    return 0;
  }

  private void initDataStructures(List<Absence> otherAbsences, Optional<LocalDate> dateAsToday) {

    // Gli intervalli su cui predere le assenze nel db
    this.previousYearInterval = DateUtility
            .intervalIntersection(wrContract.getContractDatabaseInterval(),
                    new DateInterval(new LocalDate(this.year - 1, 1, 1),
                            new LocalDate(this.year - 1, 12, 31)));
    this.requestYearInterval = DateUtility
            .intervalIntersection(wrContract.getContractDatabaseInterval(),
                    new DateInterval(new LocalDate(this.year, 1, 1),
                            new LocalDate(this.year, 12, 31)));
    this.nextYearInterval = DateUtility
            .intervalIntersection(wrContract.getContractDatabaseInterval(),
                    new DateInterval(new LocalDate(this.year + 1, 1, 1),
                            new LocalDate(this.year + 1, 12, 31)));

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

    absencesForVacationsRecap.addAll(otherAbsences);
    // TODO: filtrare otherAbsencs le sole nell'intervallo[dateFrom, dateTo]

    AbsenceType ab32 = absenceTypeManager.getAbsenceType(
            AbsenceTypeMapping.FERIE_ANNO_CORRENTE.getCode());
    AbsenceType ab31 = absenceTypeManager.getAbsenceType(
            AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE.getCode());
    AbsenceType ab37 = absenceTypeManager.getAbsenceType(
            AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE_DOPO_31_08.getCode());
    AbsenceType ab94 = absenceTypeManager.getAbsenceType(
            AbsenceTypeMapping.FESTIVITA_SOPPRESSE.getCode());


    for (Absence ab : absencesForVacationsRecap) {

      int abYear;

      if (ab.personDay != null) {
        abYear = ab.personDay.date.getYear();
      } else {
        abYear = ab.date.getYear();
      }

      //32
      if (ab.absenceType.id.equals(ab32.id)) {
        if (dateAsToday.isPresent()
                && ab.personDay.date.isAfter(dateAsToday.get())) {
          continue;
        }
        if (abYear == year - 1) {
          list32PreviouYear.add(ab);
        } else if (abYear == year) {
          list32RequestYear.add(ab);
        }
        continue;
      }
      //31
      if (ab.absenceType.id.equals(ab31.id)) {
        if (dateAsToday.isPresent()
                && ab.personDay.date.isAfter(dateAsToday.get())) {
          continue;
        }
        if (abYear == year) {
          list31RequestYear.add(ab);
        } else if (abYear == year + 1) {
          list31NextYear.add(ab);
        }
        continue;
      }
      //94
      if (ab.absenceType.id.equals(ab94.id)) {
        if (dateAsToday.isPresent()
                && ab.personDay.date.isAfter(dateAsToday.get())) {
          continue;
        }

        if (abYear == year) {
          list94RequestYear.add(ab);
        }
        continue;
      }
      //37
      if (ab.absenceType.id.equals(ab37.id)) {
        if (dateAsToday.isPresent()
                && ab.personDay.date.isAfter(dateAsToday.get())) {
          continue;
        }
        if (abYear == year) {
          list37RequestYear.add(ab);
        } else if (abYear == year + 1) {
          list37NextYear.add(ab);
        }
        continue;
      }
      //Post Partum
      postPartum.add(ab);
    }

    //Vacation Last Year Expired
    this.isExpireLastYear = false;
    if (this.year < LocalDate.now().getYear()) {
      this.isExpireLastYear = true;
    } else if (this.year == LocalDate.now().getYear()
            && accruedDate.isAfter(this.dateExpireLastYear)) {
      this.isExpireLastYear = true;
    }

    //Contract Expire Before End Of Year / Active After Begin Of Year
    LocalDate startRequestYear = new LocalDate(this.year, 1, 1);
    LocalDate endRequestYear = new LocalDate(this.year, 12, 31);
    if (this.activeContractInterval.getEnd().isBefore(endRequestYear)) {
      this.isExpireBeforeEndYear = true;
    }
    if (this.activeContractInterval.getBegin().isAfter(startRequestYear)) {
      this.isActiveAfterBeginYear = true;
    }
  }


}
