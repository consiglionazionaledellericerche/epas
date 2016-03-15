package manager.services.vacations;

import com.google.common.collect.Lists;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import models.Absence;
import models.Contract;
import models.enumerate.AbsenceTypeMapping;

import java.util.List;

/**
 * Costruisce in modo efficiente i dati temporanei per il calcolo del vacation recap.
 * 
 * @author alessandro
 */
@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PACKAGE)
public class VacationsRecapTempData {
  
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
  
  @Builder
  /**
   * Oggetto che contiene i dati di supporto al calcolo del recap ferie.
   * @param year anno
   * @param absencesToConsider assenze fatte da considerare
   * @param contract contratto
   */
  protected VacationsRecapTempData(int year, List<Absence> absencesToConsider, Contract contract) { 

    // TODO: filtrare otherAbsencs le sole nell'intervallo[dateFrom, dateTo]

    for (Absence ab : absencesToConsider) {

      int abYear;

      if (ab.getPersonDay() != null) {
        abYear = ab.getPersonDay().getDate().getYear();
      } else {
        abYear = ab.getDate().getYear();
      }

      //32
      if (ab.getAbsenceType().getCode()
          .equals(AbsenceTypeMapping.FERIE_ANNO_CORRENTE.getCode())) {
        if (abYear == year - 1) {
          this.list32PreviouYear.add(ab);
        } else if (abYear == year) {
          this.list32RequestYear.add(ab);
        }
        continue;
      }
      //31
      if (ab.getAbsenceType().getCode()
          .equals(AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE.getCode())) {
        if (abYear == year) {
          this.list31RequestYear.add(ab);
        } else if (abYear == year + 1) {
          this.list31NextYear.add(ab);
        }
        continue;
      }
      //94
      if (ab.getAbsenceType().getCode()
          .equals(AbsenceTypeMapping.FESTIVITA_SOPPRESSE.getCode())) {
        if (abYear == year) {
          this.list94RequestYear.add(ab);
        }
        continue;
      }
      //37
      if (ab.getAbsenceType().getCode()
          .equals(AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE_DOPO_31_08.getCode())) {
        if (abYear == year) {
          this.list37RequestYear.add(ab);
        } else if (abYear == year + 1) {
          this.list37NextYear.add(ab);
        }
        continue;
      }
      //Post Partum
      this.postPartum.add(ab);
    }
    
    //TODO farli diventare un getter di contract
    if (contract.getSourceDateResidual() != null
        && contract.getSourceDateResidual().getYear() == year) {

      this.sourceVacationLastYearUsed += contract.getSourceVacationLastYearUsed();
      this.sourceVacationCurrentYearUsed += contract.getSourceVacationCurrentYearUsed();
      this.sourcePermissionUsed += contract.getSourcePermissionUsed();

    }
  }
}