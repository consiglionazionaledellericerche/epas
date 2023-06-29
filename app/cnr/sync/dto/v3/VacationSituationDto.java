package cnr.sync.dto.v3;

import cnr.sync.dto.v2.ContractShowTerseDto;
import helpers.JodaConverters;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;
import manager.services.absences.model.VacationSituation;

/**
 * Contiene lo stato ferie annuale di un contratto.
 *
 * @author Cristian Lucchesi
 *
 */
@Builder
@Data
public class VacationSituationDto {

  private ContractShowTerseDto contract;
  private int year;
  private LocalDate date;

  private VacationSummaryDto lastYear;
  private VacationSummaryDto currentYear;
  private VacationSummaryDto permissions;
  
  /**
   * Costruisce un DTO a partire dal VacationSituation.
   */
  public static VacationSituationDto build(VacationSituation vacationSituation) {
    return VacationSituationDto.builder()
        .contract(ContractShowTerseDto.build(vacationSituation.contract))
        .year(vacationSituation.year)
        .date(JodaConverters.jodaToJavaLocalDate(vacationSituation.date))
        .lastYear(vacationSituation.lastYear != null ? VacationSummaryDto.build(vacationSituation.lastYear) : null)
        .currentYear(VacationSummaryDto.build(vacationSituation.currentYear))
        .permissions(VacationSummaryDto.build(vacationSituation.permissions))
        .build();
  }
}
