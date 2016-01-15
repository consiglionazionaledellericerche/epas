package manager.services.wrapperRecap;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;

import manager.services.mealTickets.wrapper.IWrapperMealTicketRecap;
import manager.services.mealTickets.wrapper.WrapperMealTicketRecap;

/**
 * @author alessandro
 */
public class WrapperRecapConfigure extends AbstractModule {

  @Override
  protected void configure() {
    install(new FactoryModuleBuilder()
            .implement(IWrapperMealTicketRecap.class, WrapperMealTicketRecap.class)
            .build(IWrapperRecapFactory.class));
  }
}
