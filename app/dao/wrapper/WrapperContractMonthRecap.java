package dao.wrapper;

import org.joda.time.YearMonth;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import models.ContractMonthRecap;

/**
 * @author alessandro
 */
public class WrapperContractMonthRecap implements IWrapperContractMonthRecap {

  private final ContractMonthRecap value;
  private final IWrapperContract contract;
  private final IWrapperFactory wrapperFactory;
  private Optional<ContractMonthRecap> previousRecap = null;

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

    if (this.previousRecap == null) {

      this.previousRecap = wrapperFactory.create(value.contract)
              .getContractMonthRecap(new YearMonth(value.year, value.month)
                      .minusMonths(1));
    }
    return this.previousRecap;
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

    if (getPreviousRecap().isPresent()) {

      if (value.month == 1) {
        return value.initMonteOreAnnoPassato;
      } else {
        return getPreviousRecap().get().remainingMinutesLastYear;
      }

    } else {
      return this.value.initMonteOreAnnoPassato;
    }
  }

}
