package manager.vacations;

import com.google.common.collect.Lists;

import dao.AbsenceDao;
import dao.wrapper.IWrapperContract;

import it.cnr.iit.epas.DateInterval;

import lombok.Builder;
import lombok.Getter;

import manager.cache.AbsenceTypeManager;

import models.Absence;
import models.Contract;
import models.Person;
import models.VacationPeriod;

import org.joda.time.LocalDate;

import java.util.List;

public class VacationRecapImpl implements IVacationRecap {

  // DATI DELLA RICHIESTA
  private int year;
  private DateInterval contractInterval = null;
  private List<VacationPeriod> contractVacationPeriods;
  private List<Absence> absencesToConsider;
  private LocalDate accruedDate;
  private LocalDate dateExpireLastYear;
  private boolean considerDateExpireLastYear;
  private LocalDate dateAsToday;
  
  private VacationRecapImpl(int year, DateInterval contractInterval, 
      List<VacationPeriod> contractVacationPeriods, List<Absence> absencesToConsider,
      LocalDate accruedDate, LocalDate dateExpireLastYear, boolean considerDateExpireLastYear,
      LocalDate dateAsToday ) {
    this.year = year;
    this.contractInterval = contractInterval;
    this.contractVacationPeriods = contractVacationPeriods;
    this.absencesToConsider = absencesToConsider;
    this.accruedDate = accruedDate;
    this.dateExpireLastYear = dateExpireLastYear;
    this.considerDateExpireLastYear = considerDateExpireLastYear;
    this.dateAsToday = dateAsToday;
    
  }
  
  @Builder
  public static VacationRecapImpl builder(int year, DateInterval contractInterval, 
      List<VacationPeriod> contractVacationPeriods, List<Absence> absencesToConsider, 
      LocalDate dateExpireLastYear) {
    return new VacationRecapImpl(year,contractInterval, contractVacationPeriods, absencesToConsider,
        LocalDate.now(), dateExpireLastYear, true, LocalDate.now()); 
  }
  
  
  // DECISIONI
  
  private AccruedDecision decisionsVacationLastYearAccrued;
  private AccruedDecision decisionsVacationCurrentYearAccrued;
  private AccruedDecision decisionsPermissionYearAccrued;
  private AccruedDecision decisionsVacationCurrentYearTotal;
  private AccruedDecision decisionsPermissionYearTotal;

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
  
  private DateInterval previousYearInterval;
  private DateInterval requestYearInterval;
  private DateInterval nextYearInterval;
  private List<Absence> list32PreviouYear = Lists.newArrayList();
  private List<Absence> list31RequestYear = Lists.newArrayList();
  private List<Absence> list37RequestYear = Lists.newArrayList();
  private List<Absence> list32RequestYear = Lists.newArrayList();
  private List<Absence> list31NextYear = Lists.newArrayList();
  private List<Absence> list37NextYear = Lists.newArrayList();
  private List<Absence> list94RequestYear = Lists.newArrayList();
  private List<Absence> postPartum = Lists.newArrayList();
  
}
