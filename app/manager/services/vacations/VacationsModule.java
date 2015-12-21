package manager.services.vacations;

import com.google.inject.AbstractModule;

import manager.services.vacations.impl.RealVacationsService;

public class VacationsModule extends AbstractModule {
  
  @Override 
  protected void configure() {
    
    //bind(TransactionLog.class).to(DatabaseTransactionLog.class);
    //bind(CreditCardProcessor.class).to(PaypalCreditCardProcessor.class);
    //bind(VacationsService.class).to(RealVacationService.class);
    
    bind(IVacationsService.class).to(RealVacationsService.class);
    
  }
}