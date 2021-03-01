package cnr.sync.dto.v3;

import lombok.Builder;
import lombok.Data;
import manager.services.absences.model.VacationSituation.VacationSummary;

/**
 * DTO contenente il resoconto della situazione delle ferie.
 *
 * @author Cristian Lucchesi
 *
 */
@Builder
@Data
public class VacationSummaryDto {
  
  private Integer year;
  private Integer total;
  private Integer accrued;
  private Integer used;
  private Integer usableTotal;
  private Integer usable;
  
  /**
   * Costruisce un DTO a partire dal VacationSummary.
   */
  public static VacationSummaryDto build(VacationSummary vacationSummury) {
    return VacationSummaryDto.builder()
        .year(vacationSummury.year)
        .total(vacationSummury.total())
        .accrued(vacationSummury.accrued())
        .used(vacationSummury.used())
        .usableTotal(vacationSummury.usableTotal())
        .usable(vacationSummury.usable())
        .build();
  }
}
