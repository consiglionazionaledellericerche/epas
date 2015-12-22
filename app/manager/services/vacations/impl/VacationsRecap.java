package manager.services.vacations.impl;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

import it.cnr.iit.epas.DateInterval;

import lombok.Builder;
import lombok.Getter;

import manager.services.vacations.IVacationsRecap;
import manager.services.vacations.impl.VacationsTypeResult.TypeVacation;

import models.Absence;
import models.Contract;
import models.VacationPeriod;
import models.enumerate.AbsenceTypeMapping;

import org.joda.time.LocalDate;

import java.util.List;

/**
 * Implementazione dell'interfaccia IVacationsRecap. 
 * Contiene il riepilogo ferie per un certo anno di un contratto.
 * 
 * @author alessandro
 *
 */
public class VacationsRecap implements IVacationsRecap {

  /**
   * Raccoglie i dati della richiesta necessari al calcolo per la 
   * costruzione riepilogo ferie e permessi.
   * @author alessandro
   *
   */
  public static class VacationsRequest {
    
    @Getter private final int year;
    @Getter private final DateInterval contractDateInterval;
    @Getter private final LocalDate accruedDate;
    @Getter private final List<VacationPeriod> contractVacationPeriod;
    @Getter private final List<Absence> postPartumUsed;
    @Getter private final LocalDate expireDate;
    @Getter private final boolean considerExpireDate;

    @Builder
    private VacationsRequest(final int year, final DateInterval contractDateInterval, 
        final Optional<LocalDate> accruedDate, final List<VacationPeriod> contractVacationPeriod, 
        final List<Absence> postPartumUsed, final LocalDate expireDate, 
        final boolean considerExpireDate) {
      this.contractVacationPeriod = contractVacationPeriod;
      this.year = year;
      this.contractDateInterval = contractDateInterval;
      this.postPartumUsed = postPartumUsed;
      if (accruedDate.isPresent()) {
        this.accruedDate = accruedDate.get();
      } else {
        this.accruedDate = LocalDate.now();  
      }
      this.expireDate = expireDate;
      this.considerExpireDate = considerExpireDate;
    }
  }
  
  @Getter private VacationsRequest vacationsRequest;
  
  // DECISIONI
  @Getter private VacationsTypeResult vacationsLastYear;
  @Getter private VacationsTypeResult vacationsCurrentYear;
  @Getter private VacationsTypeResult permissions;

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
  private List<Absence> list32PreviouYear = Lists.newArrayList();
  private List<Absence> list31RequestYear = Lists.newArrayList();
  private List<Absence> list37RequestYear = Lists.newArrayList();
  private List<Absence> list32RequestYear = Lists.newArrayList();
  private List<Absence> list31NextYear = Lists.newArrayList();
  private List<Absence> list37NextYear = Lists.newArrayList();
  private List<Absence> list94RequestYear = Lists.newArrayList();
  private List<Absence> postPartum = Lists.newArrayList();
  private int sourceVacationLastYearUsed = 0;
  private int sourceVacationCurrentYearUsed = 0;
  private int sourcePermissionUsed = 0;
  
  /**
   * Costruttore.
   * @param year anno
   * @param contract contratto
   * @param absencesToConsider assenze da considerare
   * @param accruedDate data di maturazione
   * @param expireDate data di scadenza ferie
   * @param considerDateExpireLastYear se considerare la data si scadenza
   * @param dateAsToday simulazione come se fosse oggi.
   */
  public VacationsRecap(int year, Contract contract, List<Absence> absencesToConsider,
      LocalDate accruedDate, LocalDate expireDate, boolean considerDateExpireLastYear,
      Optional<LocalDate> dateAsToday ) {
    
    initDataStructures(absencesToConsider, dateAsToday, contract);
    
    this.vacationsRequest = VacationsRequest.builder()
        .year(year)
        .contractDateInterval(new DateInterval(contract.getBeginDate(), contract.calculatedEnd()))
        .accruedDate(Optional.fromNullable(accruedDate))
        .contractVacationPeriod(contract.vacationPeriods)
        .postPartumUsed(this.postPartum)
        .expireDate(expireDate)
        .considerExpireDate(considerDateExpireLastYear).build();

    this.vacationsLastYear = VacationsTypeResult.builder()
        .vacationsRequest(vacationsRequest)
        .absencesUsed(FluentIterable
            .from(list32PreviouYear)
            .append(list31RequestYear)
            .append(list37RequestYear).toList())
        .sourced(sourceVacationLastYearUsed)
        .typeVacation(TypeVacation.VACATION_LAST_YEAR)
        .build();
    
    this.vacationsCurrentYear = VacationsTypeResult.builder()
        .vacationsRequest(vacationsRequest)
        .absencesUsed(FluentIterable
            .from(list32RequestYear)
            .append(list31NextYear)
            .append(list37NextYear).toList())
        .sourced(sourceVacationCurrentYearUsed)
        .typeVacation(TypeVacation.VACATION_LAST_YEAR)
        .build();
    
    this.permissions = VacationsTypeResult.builder()
        .vacationsRequest(vacationsRequest)
        .absencesUsed(FluentIterable
            .from(list94RequestYear).toList())
        .sourced(sourcePermissionUsed)
        .typeVacation(TypeVacation.VACATION_LAST_YEAR)
        .build();
  }
  
  /**
   * Inizializza le strutture per il calcolo.
   *  
   * @param absencesToConsider la lista di assenza fatte da considerare.
   */
  private void initDataStructures(List<Absence> absencesToConsider,
      Optional<LocalDate> dateAsToday, Contract contract) {
   
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
        if (abYear == vacationsRequest.year - 1) {
          list32PreviouYear.add(ab);
        } else if (abYear == vacationsRequest.year) {
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
        if (abYear == vacationsRequest.year) {
          list31RequestYear.add(ab);
        } else if (abYear == vacationsRequest.year + 1) {
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
        if (abYear == vacationsRequest.year) {
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
        if (abYear == vacationsRequest.year) {
          list37RequestYear.add(ab);
        } else if (abYear == vacationsRequest.year + 1) {
          list37NextYear.add(ab);
        }
        continue;
      }
      //Post Partum
      postPartum.add(ab);
    }

    //Vacation Last Year Expired
    this.isExpireLastYear = false;
    if (vacationsRequest.year < LocalDate.now().getYear()) {
      this.isExpireLastYear = true;
    } else if (vacationsRequest.year == LocalDate.now().getYear()
            && vacationsRequest.accruedDate.isAfter(vacationsRequest.expireDate)) {
      this.isExpireLastYear = true;
    }

    //Contract Expire Before End Of Year / Active After Begin Of Year
    LocalDate startRequestYear = new LocalDate(vacationsRequest.year, 1, 1);
    LocalDate endRequestYear = new LocalDate(vacationsRequest.year, 12, 31);
    if (this.contractInterval.getEnd().isBefore(endRequestYear)) {
      this.isExpireBeforeEndYear = true;
    }
    if (this.contractInterval.getBegin().isAfter(startRequestYear)) {
      this.isActiveAfterBeginYear = true;
    }
    
    //TODO farli diventare un getter di contract
    if (contract.sourceDateResidual != null 
        && contract.sourceDateResidual.getYear() == vacationsRequest.year) {
      
      sourceVacationLastYearUsed += contract.sourceVacationLastYearUsed;
      sourceVacationCurrentYearUsed += contract.sourceVacationCurrentYearUsed;
      sourcePermissionUsed += contract.sourcePermissionUsed;
      
    }
    
  }
    
}
