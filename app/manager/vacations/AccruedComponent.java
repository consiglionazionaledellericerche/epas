package manager.vacations;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import models.Absence;
import models.VacationPeriod;

import org.joda.time.LocalDate;

import java.util.List;

public class AccruedComponent {
  
  @Getter private final int year;
  @Getter private final DateInterval contractDateInterval;
  @Getter private final Optional<LocalDate> accruedDate;
  @Getter private final List<VacationPeriod> contractVacationPeriod;
  @Getter private final List<Absence> postPartum;
     
  /**
   * Costruttore privato.
   * @param year anno
   * @param contractDateInterval durata contratto
   * @param accruedDate data fine maturazione.
   */
  
  /**
   * Costruisce il componente.
   * @param year anno
   * @param contractDateInterval intervallo contratto
   * @param accruedDate data fine maturazione
   * @param contractVacationPeriod i piani ferie del contratto
   * @param postPartum le assenze fatte post partum (da filtrare)
   */
  @Builder
  private AccruedComponent(int year, DateInterval contractDateInterval, 
      Optional<LocalDate> accruedDate, List<VacationPeriod> contractVacationPeriod, 
      List<Absence> postPartum) {
    this.contractVacationPeriod = contractVacationPeriod;
    this.postPartum = postPartum;
    this.year = year;
    this.contractDateInterval = contractDateInterval;
    this.accruedDate = accruedDate;
  }
    
}


