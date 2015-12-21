package dao.wrapper;

import com.google.common.base.Optional;

import it.cnr.iit.epas.DateInterval;

import models.Contract;
import models.ContractMonthRecap;
import models.ContractWorkingTimeType;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import java.util.List;

public interface IWrapperContract extends IWrapperModel<Contract> {

  boolean isLastInMonth(int month, int year);

  /**
   * True se il contratto è a tempo determinato.
   */
  boolean isDefined();

  DateInterval getContractDateInterval();

  DateInterval getContractDatabaseInterval();

  DateInterval getContractDatabaseIntervalForMealTicket();

  Optional<YearMonth> getFirstMonthToRecap();

  YearMonth getLastMonthToRecap();

  List<ContractWorkingTimeType> getContractWorkingTimeTypeAsList();

  Optional<ContractMonthRecap> getContractMonthRecap(YearMonth yearMonth);

  /**
   * Diagnostiche sul contratto.
   */
  public boolean noRelevant();

  public boolean initializationMissing();

  public LocalDate dateForInitialization();

  public boolean monthRecapMissing(YearMonth yearMonth);

  public boolean monthRecapMissing();

  public boolean hasMonthRecapForVacationsRecap(int yearToRecap);

}
