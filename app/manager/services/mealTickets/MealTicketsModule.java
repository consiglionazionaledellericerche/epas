package manager.services.mealTickets;

import com.google.inject.AbstractModule;

public class MealTicketsModule extends AbstractModule {
  
  @Override 
  protected void configure() {
    
    bind(IMealTicketsService.class).to(MealTicketsServiceImpl.class);
    
  }
}