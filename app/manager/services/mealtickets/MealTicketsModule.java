package manager.services.mealtickets;

import com.google.inject.AbstractModule;
import injection.AutoRegister;

@AutoRegister
public class MealTicketsModule extends AbstractModule {
  
  @Override 
  protected void configure() {
    
    bind(IMealTicketsService.class).to(MealTicketsServiceImpl.class);
    
  }
}