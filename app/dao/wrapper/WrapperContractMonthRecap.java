/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dao.wrapper;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import models.ContractMonthRecap;
import org.joda.time.YearMonth;

/**
 * Implementazione contractMonthRecap.
 *
 * @author Alessandro Martelli
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
    this.contract = wrapperFactory.create(cmr.getContract());
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
    return wrapperFactory.create(value.getContract())
              .getContractMonthRecap(new YearMonth(value.getYear(), value.getMonth())
                      .minusMonths(1));
  }
  
  /**
   * Il recap precedente se presente.
   */
  @Override
  public Optional<ContractMonthRecap> getPreviousRecapInYear() {
    
    if (this.value.getMonth() != 1) {
      return wrapperFactory.create(value.getContract())
              .getContractMonthRecap(new YearMonth(value.getYear(), value.getMonth())
                      .minusMonths(1));
    }
    return Optional.<ContractMonthRecap>absent();
  }

  /**
   * Se visualizzare il prospetto sul monte ore anno precedente.
   */
  @Override
  public boolean hasResidualLastYear() {

    return value.isPossibileUtilizzareResiduoAnnoPrecedente();
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

      if (value.getMonth() == 1) {
        return value.getInitMonteOreAnnoPassato();
      } else {
        return previous.get().getRemainingMinutesLastYear();
      }

    } else {
      return this.value.getInitMonteOreAnnoPassato();
    }
  }

}
