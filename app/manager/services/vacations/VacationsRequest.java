package manager.services.vacations;

import com.google.common.base.Optional;

import it.cnr.iit.epas.DateInterval;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import models.Absence;
import models.Contract;
import models.VacationPeriod;

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