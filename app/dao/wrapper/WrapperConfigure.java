package dao.wrapper;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import injection.AutoRegister;

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
