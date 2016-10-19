package manager.services.vacations;

import it.cnr.iit.epas.DateInterval;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import models.Contract;
import models.VacationPeriod;
import models.absences.Absence;

import org.joda.time.LocalDate;

import java.util.List;

/**
 * Raccoglie i dati della richiesta necessari al calcolo per la
 * costruzione riepilogo ferie e permessi.
 * @author alessandro
 *
 */
@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PACKAGE)
public class VacationsRequest {

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
      final DateInterval contractDateInterval, final LocalDate accruedDate,
      final List<VacationPeriod> contractVacationPeriod, final List<Absence> postPartumUsed,
      final LocalDate expireDateLastYear, final LocalDate expireDateCurrentYear) {
    this.year = year;
    this.contract = contract;
    this.contractVacationPeriod = contractVacationPeriod;
    this.contractDateInterval = contractDateInterval;
    this.postPartumUsed = postPartumUsed;
    this.accruedDate = accruedDate;
    this.expireDateLastYear = expireDateLastYear;
    this.expireDateCurrentYear = expireDateCurrentYear;
  }
}