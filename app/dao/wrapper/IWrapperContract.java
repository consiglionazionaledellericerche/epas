package dao.wrapper;

import java.util.List;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import com.google.common.base.Optional;

import it.cnr.iit.epas.DateInterval;
import models.Contract;
import models.ContractMonthRecap;
import models.ContractWorkingTimeType;

public interface IWrapperContract extends IWrapperModel<Contract> {

  /**
   *
   * @param month
   * @param year
   * @return
   */
  boolean isLastInMonth(int month, int year);

  /**
   * True se il contratto è a tempo determinato.
   */
  boolean isDefined();

  /**
   *
   * @return
   */
  DateInterval getContractDateInterval();

  /**
   *
   * @return
   */
  DateInterval getContractDatabaseInterval();

  /**
   *
   * @return
   */
  DateInterval getContractDatabaseIntervalForMealTicket();

  /**
   *
   * @return
   */
  Optional<YearMonth> getFirstMonthToRecap();

  /**
   *
   * @return
   */
  YearMonth getLastMonthToRecap();

  /**
   *
   * @return
   */
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
