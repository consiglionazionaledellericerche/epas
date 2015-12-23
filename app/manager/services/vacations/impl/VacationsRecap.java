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
    @Getter private final Contract contract;
    @Getter private final DateInterval contractDateInterval;
    @Getter private final LocalDate accruedDate;
    @Getter private final List<VacationPeriod> contractVacationPeriod;
    @Getter private final List<Absence> postPartumUsed;
    @Getter private final LocalDate expireDateLastYear;
    @Getter private final LocalDate expireDateCurrentYear;

    @Builder
    private VacationsRequest(final int year, final Contract contract,
        final DateInterval contractDateInterval, final Optional<LocalDate> accruedDate,
        final List<VacationPeriod> contractVacationPeriod, final List<Absence> postPartumUsed,
        final LocalDate expireDateLastYear, final LocalDate expireDateCurrentYear) {
      this.year = year;
      this.contract = contract;
      this.contractVacationPeriod = contractVacationPeriod;
      this.contractDateInterval = contractDateInterval;
      this.postPartumUsed = postPartumUsed;
      if (accruedDate.isPresent()) {
        this.accruedDate = accruedDate.get();
      } else {
        this.accruedDate = LocalDate.now();
      }
      this.expireDateLastYear = expireDateLastYear;
      this.expireDateCurrentYear = expireDateCurrentYear;
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
   * @param expireDateLastYear data di scadenza ferie
   * @param expireDateCurrentYear data di scadenza ferie
   */
  @Builder
  public VacationsRecap(int year, Contract contract, List<Absence> absencesToConsider,
      LocalDate accruedDate, LocalDate expireDateLastYear, LocalDate expireDateCurrentYear) {

    DateInterval contractDateInterval =
        new DateInterval(contract.getBeginDate(), contract.calculatedEnd());

    initDataStructures(year, accruedDate, expireDateLastYear, absencesToConsider, contract,
        contractDateInterval);

    this.vacationsRequest = VacationsRequest.builder()
        .year(year)
        .contract(contract)
        .contractDateInterval(contractDateInterval)
        .accruedDate(Optional.fromNullable(accruedDate))
        .contractVacationPeriod(contract.getVacationPeriods())
        .postPartumUsed(this.postPartum)
        .expireDateLastYear(expireDateLastYear)
        .expireDateCurrentYear(expireDateCurrentYear)
        .build();

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
        .typeVacation(TypeVacation.VACATION_CURRENT_YEAR)
        .build();

    this.permissions = VacationsTypeResult.builder()
        .vacationsRequest(vacationsRequest)
        .absencesUsed(FluentIterable
            .from(list94RequestYear).toList())
        .sourced(sourcePermissionUsed)
        .typeVacation(TypeVacation.PERMISSION_CURRENT_YEAR)
        .build();
  }

  /**
   * Inizializza le strutture per il calcolo (in modo efficiente).
   *
   * @param absencesToConsider la lista di assenza fatte da considerare.
   */
  private void initDataStructures(int year, LocalDate accruedDate, LocalDate expireDate,
      List<Absence> absencesToConsider, Contract contract,
      DateInterval contractDateInterval) {

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
        if (abYear == year - 1) {
          list32PreviouYear.add(ab);
        } else if (abYear == year) {
          list32RequestYear.add(ab);
        }
        continue;
      }
      //31
      if (ab.absenceType.code.equals(AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE.getCode())) {
        if (abYear == year) {
          list31RequestYear.add(ab);
        } else if (abYear == year + 1) {
          list31NextYear.add(ab);
        }
        continue;
      }
      //94
      if (ab.absenceType.code.equals(AbsenceTypeMapping.FESTIVITA_SOPPRESSE.getCode())) {
        if (abYear == year) {
          list94RequestYear.add(ab);
        }
        continue;
      }
      //37
      if (ab.absenceType.code.equals(AbsenceTypeMapping
          .FERIE_ANNO_PRECEDENTE_DOPO_31_08.getCode())) {
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
    if (year < LocalDate.now().getYear()) {
      this.isExpireLastYear = true;
    } else if (year == LocalDate.now().getYear()
            && accruedDate.isAfter(expireDate)) {
      this.isExpireLastYear = true;
    }

    //Contract Expire Before End Of Year / Active After Begin Of Year
    LocalDate startRequestYear = new LocalDate(year, 1, 1);
    LocalDate endRequestYear = new LocalDate(year, 12, 31);
    if (contractDateInterval.getEnd().isBefore(endRequestYear)) {
      this.isExpireBeforeEndYear = true;
    }
    if (contractDateInterval.getBegin().isAfter(startRequestYear)) {
      this.isActiveAfterBeginYear = true;
    }

    //TODO farli diventare un getter di contract
    if (contract.getSourceDateResidual() != null
        && contract.getSourceDateResidual().getYear() == year) {

      sourceVacationLastYearUsed += contract.getSourceVacationLastYearUsed();
      sourceVacationCurrentYearUsed += contract.getSourceVacationCurrentYearUsed();
      sourcePermissionUsed += contract.getSourcePermissionUsed();

    }

  }

}
