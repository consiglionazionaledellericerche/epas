package manager.services.vacations;

import com.google.inject.AbstractModule;

import manager.services.vacations.impl.VacationsService;

public class VacationsModule extends AbstractModule {
  
  @Override 
  protected void configure() {
    
    bind(IVacationsService.class).to(VacationsService.class);
    
  }
}