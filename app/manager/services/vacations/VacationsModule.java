package manager.services.vacations;

import com.google.inject.AbstractModule;

public class VacationsModule extends AbstractModule {
  
  @Override 
  protected void configure() {
    
    bind(IVacationsService.class).to(VacationsServiceImpl.class);
    
  }
}