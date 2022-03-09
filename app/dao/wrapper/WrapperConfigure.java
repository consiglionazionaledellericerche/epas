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

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import common.injection.AutoRegister;

/**
 * Modulo per la registrazione dei Wrapper da utilizzare via Injection.
 */
@AutoRegister
public class WrapperConfigure extends AbstractModule {

  @Override
  protected void configure() {
    install(new FactoryModuleBuilder()
            .implement(IWrapperPerson.class, WrapperPerson.class)
            .implement(IWrapperContract.class, WrapperContract.class)
            .implement(IWrapperWorkingTimeType.class, WrapperWorkingTimeType.class)
            .implement(IWrapperCompetenceCode.class, WrapperCompetenceCode.class)
            .implement(IWrapperOffice.class, WrapperOffice.class)
            .implement(IWrapperPersonDay.class, WrapperPersonDay.class)
            .implement(IWrapperContractMonthRecap.class, WrapperContractMonthRecap.class)
            .implement(IWrapperContractWorkingTimeType.class, WrapperContractWorkingTimeType.class)
            .implement(IWrapperTimeSlot.class, WrapperTimeSlot.class)
            .build(IWrapperFactory.class));
  }
}
