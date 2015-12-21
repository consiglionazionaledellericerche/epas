package manager.services.vacations;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import it.cnr.iit.epas.DateInterval;

import manager.services.vacations.impl.AccruedDecision;

import models.Absence;
import models.Contract;

import org.joda.time.LocalDate;

import java.util.List;

public interface IVacationsRecap {
  
  // DATI DELLA RICHIESTA
  int getYear();
  
  Contract getContract();
  
  List<Absence> getAbsencesToConsider();
  
  LocalDate getAccruedDate();
  
  LocalDate getDateExpireLastYear();
  
  boolean isConsiderDateExpireLastYear();
  
  Optional<LocalDate> getDateAsToday();

  // DECISIONI
  AccruedDecision getDecisionsVacationLastYearAccrued();
  
  AccruedDecision getDecisionsVacationCurrentYearAccrued();
  
  AccruedDecision getDecisionsPermissionYearAccrued();
  
  AccruedDecision getDecisionsVacationCurrentYearTotal();
  
  AccruedDecision getDecisionsPermissionYearTotal();
  
  // TOTALI
  Integer getVacationDaysCurrentYearTotal();
  
  Integer getPermissionCurrentYearTotal();

  // USATE
  int getVacationDaysLastYearUsed();
  
  int getVacationDaysCurrentYearUsed();
  
  int getPermissionUsed();

  // MATURATE
  Integer getVacationDaysLastYearAccrued();
  
  Integer getVacationDaysCurrentYearAccrued();
  
  Integer getPermissionCurrentYearAccrued();

  // RIMANENTI
  Integer getVacationDaysLastYearNotYetUsed();
  
  Integer getVacationDaysCurrentYearNotYetUsed();
  
  Integer getPersmissionNotYetUsed();

  /**
   * True se le ferie dell'anno passato sono scadute.
   */
  boolean isExpireLastYear();

  /**
   * True se il contratto scade prima della fine dell'anno.
   */
  boolean isExpireBeforeEndYear();

  /**
   * True se il contratto inizia dopo l'inizio dell'anno.
   */
  boolean isActiveAfterBeginYear();
}
