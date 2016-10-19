package dao.wrapper;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import models.ContractMonthRecap;

import org.joda.time.YearMonth;

/**
 * @author alessandro.
 */
public class WrapperContractMonthRecap implements IWrapperContractMonthRecap {

  private final ContractMonthRecap value;
  private final IWrapperContract contract;
  private final IWrapperFactory wrapperFactory;

  @Inject
  WrapperContractMonthRecap(@Assisted ContractMonthRecap cmr,
                            IWrapperFactory wrapperFactory
  ) {
    this.wrapperFactory = wrapperFactory;
    this.contract = wrapperFactory.create(cmr.contract);
    this.value = cmr;

  }

  @Override
  public ContractMonthRecap getValue() {
    return value;
  }

  @Override
  public IWrapperContract getContract() {
    return contract;
  }


  /**
   * Il recap precedente se presente. Istanzia una variabile lazy.
   */
  @Override
  public Optional<ContractMonthRecap> getPreviousRecap() {
    return wrapperFactory.create(value.contract)
              .getContractMonthRecap(new YearMonth(value.year, value.month)
                      .minusMonths(1));
  }
  
  /**
   * Il recap precedente se presente.
   */
  @Override
  public Optional<ContractMonthRecap> getPreviousRecapInYear() {
    
    if (this.value.month != 1) {
      return wrapperFactory.create(value.contract)
              .getContractMonthRecap(new YearMonth(value.year, value.month)
                      .minusMonths(1));
    }
    return Optional.<ContractMonthRecap>absent();
  }

  /**
   * Se visualizzare il prospetto sul monte ore anno precedente.
   */
  @Override
  public boolean hasResidualLastYear() {

    return value.possibileUtilizzareResiduoAnnoPrecedente;
  }

  /**
   * Il valore iniziale del monte ore anno precedente.
   */
  @Override
  public int getResidualLastYearInit() {

    if (!hasResidualLastYear()) {
      return 0;
    }
    //Preconditions.checkState(hasResidualLastYear());
    Optional<ContractMonthRecap> previous = getPreviousRecap();
    if (previous.isPresent()) {

      if (value.month == 1) {
        return value.initMonteOreAnnoPassato;
      } else {
        return previous.get().remainingMinutesLastYear;
      }

    } else {
      return this.value.initMonteOreAnnoPassato;
    }
  }

}
