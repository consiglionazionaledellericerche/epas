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

package dao.wrapper.function;

import com.google.common.base.Function;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperContractMonthRecap;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperOffice;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.IWrapperTimeSlot;
import dao.wrapper.IWrapperWorkingTimeType;
import javax.inject.Inject;
import models.Contract;
import models.ContractMonthRecap;
import models.Office;
import models.Person;
import models.TimeSlot;
import models.WorkingTimeType;

/**
 * Factory per alcune Function di utilità da utilizzare nei Wrapper.
 */
public class WrapperModelFunctionFactory {

  private final IWrapperFactory factory;

  @Inject
  WrapperModelFunctionFactory(IWrapperFactory factory) {
    this.factory = factory;
  }

  /**
   * Permette la creazione di un'istanza wrapperWorkingTyimeType a partire dall'oggetto
   * del modello.
   *
   * @return un wrapper di un workingTimeType.
   */
  public Function<WorkingTimeType, IWrapperWorkingTimeType> workingTimeType() {
    return new Function<WorkingTimeType, IWrapperWorkingTimeType>() {

      @Override
      public IWrapperWorkingTimeType apply(WorkingTimeType input) {
        return factory.create(input);
      }
    };
  }

  /**
   * Permette la creazione di un'istanza wrapperTimeSlot a partire dall'oggetto TimeSlot.
   *
   * @return un wrapper di un timeslot.
   */
  public Function<TimeSlot, IWrapperTimeSlot> timeSlot() {
    return new Function<TimeSlot, IWrapperTimeSlot>() {

      @Override
      public IWrapperTimeSlot apply(TimeSlot input) {
        return factory.create(input);
      }
    };
  }
  
  /**
   * Permette la creazione di un'istanza wrapperPerson a partire dall'oggetto del modello person.
   *
   * @return un wrapper di una person.
   */
  public Function<Person, IWrapperPerson> person() {
    return new Function<Person, IWrapperPerson>() {

      @Override
      public IWrapperPerson apply(Person input) {
        return factory.create(input);
      }
    };
  }

  /**
   * Permette la creazione di un'istanza wrapperOffice a partire dall'oggetto del modello office.
   *
   * @return un wrapper di un office.
   */
  public Function<Office, IWrapperOffice> office() {
    return new Function<Office, IWrapperOffice>() {

      @Override
      public IWrapperOffice apply(Office input) {
        return factory.create(input);
      }
    };
  }

  /**
   * Permette la creazione di un'istanza wrapperContract a partire dall'oggetto del modello 
   * contract.
   *
   * @return un wrapper di un contract.
   */
  public Function<Contract, IWrapperContract> contract() {
    return new Function<Contract, IWrapperContract>() {

      @Override
      public IWrapperContract apply(Contract input) {
        return factory.create(input);
      }
    };
  }

  /**
   * Permette la creazione di un'istanza wrapperContractMonthRecap a partire dall'oggetto del 
   * modello contractMonthRecap.
   *
   * @return un wrapper di un contractMonthRecap.
   */
  public Function<ContractMonthRecap, IWrapperContractMonthRecap> contractMonthRecap() {
    return new Function<ContractMonthRecap, IWrapperContractMonthRecap>() {

      @Override
      public IWrapperContractMonthRecap apply(ContractMonthRecap input) {
        return factory.create(input);
      }
    };
  }
}