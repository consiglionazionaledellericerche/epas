package manager.services.vacations.impl;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import it.cnr.iit.epas.DateInterval;

import lombok.Getter;

import manager.services.vacations.IVacationsRecap;
import manager.services.vacations.impl.AccruedDecision.TypeAccrued;

import models.Absence;
import models.Contract;
import models.enumerate.AbsenceTypeMapping;

import org.joda.time.LocalDate;

import java.util.List;

public class VacationsRecapImpl implements IVacationsRecap {

  // DATI DELLA RICHIESTA
  @Getter private int year;
  @Getter private Contract contract;
  @Getter private List<Absence> absencesToConsider;
  @Getter private LocalDate accruedDate;
  @Getter private LocalDate dateExpireLastYear;
  @Getter private boolean considerDateExpireLastYear;
  @Getter private Optional<LocalDate> dateAsToday;
  
  // DECISIONI
  @Getter private AccruedDecision decisionsVacationLastYearAccrued;
  @Getter private AccruedDecision decisionsVacationCurrentYearAccrued;
  @Getter private AccruedDecision decisionsPermissionYearAccrued;
  @Getter private AccruedDecision decisionsVacationCurrentYearTotal;
  @Getter private AccruedDecision decisionsPermissionYearTotal;

  // TOTALI
  @Getter private Integer vacationDaysCurrentYearTotal = 0;
  @Getter private Integer permissionCurrentYearTotal = 0;
  
  // USATE
  @Getter private int vacationDaysLastYearUsed = 0;
  @Getter private int vacationDaysCurrentYearUsed = 0;
  @Getter private int permissionUsed = 0;
  
  // MATURATE
  @Getter private Integer vacationDaysLastYearAccrued = 0;
  @Getter private Integer vacationDaysCurrentYearAccrued = 0;
  @Getter private Integer permissionCurrentYearAccrued = 0;
  
  // RIMANENTI
  @Getter private Integer vacationDaysLastYearNotYetUsed = 0;
  @Getter private Integer vacationDaysCurrentYearNotYetUsed = 0;
  @Getter private Integer persmissionNotYetUsed = 0;

  /**
   * True se le ferie dell'anno passato sono scadute.
   */
  @Getter private boolean isExpireLastYear = false;
  
  /**
   * True se il contratto scade prima della fine dell'anno.
   */
  @Getter private boolean isExpireBeforeEndYear = false;
  
  /**
   * True se il contratto inizia dopo l'inizio dell'anno.
   */
  @Getter private boolean isActiveAfterBeginYear = false;
  
  // SUPPORTO AL CALCOLO
  
  private DateInterval contractInterval;
  //private DateInterval previousYearInterval;
  //private DateInterval requestYearInterval;
  //private DateInterval nextYearInterval;
  private List<Absence> list32PreviouYear = Lists.newArrayList();
  private List<Absence> list31RequestYear = Lists.newArrayList();
  private List<Absence> list37RequestYear = Lists.newArrayList();
  private List<Absence> list32RequestYear = Lists.newArrayList();
  private List<Absence> list31NextYear = Lists.newArrayList();
  private List<Absence> list37NextYear = Lists.newArrayList();
  private List<Absence> list94RequestYear = Lists.newArrayList();
  private List<Absence> postPartum = Lists.newArrayList();
  
  /**
   * Costruttore.
   * @param year anno
   * @param contract contratto
   * @param absencesToConsider assenze da considerare
   * @param accruedDate data di maturazione
   * @param dateExpireLastYear data di scadenza ferie
   * @param considerDateExpireLastYear se considerare la data si scadenza
   * @param dateAsToday simulazione come se fosse oggi.
   */
  public VacationsRecapImpl(int year, Contract contract, List<Absence> absencesToConsider,
      LocalDate accruedDate, LocalDate dateExpireLastYear, boolean considerDateExpireLastYear,
      Optional<LocalDate> dateAsToday ) {
    this.year = year;
    this.contractInterval = new DateInterval(contract.getBeginDate(), contract.calculatedEnd());
    this.absencesToConsider = absencesToConsider;
    this.accruedDate = accruedDate;
    this.dateExpireLastYear = dateExpireLastYear;
    this.considerDateExpireLastYear = considerDateExpireLastYear;
    this.dateAsToday = dateAsToday;
    
    initDataStructures(absencesToConsider);
    
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
        .contractDateInterval(contractInterval)
        .accruedDate(Optional.fromNullable(accruedDate))
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
    if (!this.accruedDate.isAfter(dateExpireLastYear) || !considerDateExpireLastYear) {
      this.vacationDaysLastYearNotYetUsed = this.vacationDaysLastYearAccrued
              - this.vacationDaysLastYearUsed;
    } else {
      this.vacationDaysLastYearNotYetUsed = 0;
    }

    //Anno corrente
    if (this.contract.getEndDate() != null) {
      
      //per i determinati considero le maturate (perchè potrebbero decidere di cambiare contratto)
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
   * Inizializza le strutture per il calcolo.
   *  
   * @param absencesToConsider la lista di assenza fatte da considerare.
   */
  private void initDataStructures(List<Absence> absencesToConsider) {
   
    // TODO: filtrare otherAbsencs le sole nell'intervallo[dateFrom, dateTo]

    for (Absence ab : absencesToConsider) {

      int abYear;

      if (ab.personDay != null) {
        abYear = ab.personDay.date.getYear();
      } else {
        abYear = ab.date.getYear();
      }

      //32
      if (ab.absenceType.code.equals(AbsenceTypeMapping.FERIE_ANNO_CORRENTE.getCode())) {
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
      if (ab.absenceType.code.equals(AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE.getCode())) {
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
      if (ab.absenceType.code.equals(AbsenceTypeMapping.FESTIVITA_SOPPRESSE.getCode())) {
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
      if (ab.absenceType.code.equals(AbsenceTypeMapping
          .FERIE_ANNO_PRECEDENTE_DOPO_31_08.getCode())) {
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
    if (this.contractInterval.getEnd().isBefore(endRequestYear)) {
      this.isExpireBeforeEndYear = true;
    }
    if (this.contractInterval.getBegin().isAfter(startRequestYear)) {
      this.isActiveAfterBeginYear = true;
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
    
    // FIXME controllare questa parte.......
//    if (this.contract.sourceDateResidual != null 
//        && this.contract.sourceDateResidual.getYear() == year) {
//      
//      vacationDaysLastYearUsed += this.contract.sourceVacationLastYearUsed;
//      vacationDaysCurrentYearUsed += this.contract.sourceVacationCurrentYearUsed;
//      permissionUsed += this.contract.sourcePermissionUsed;
//      
//    }

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
  
}
