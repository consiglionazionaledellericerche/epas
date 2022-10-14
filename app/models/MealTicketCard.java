package models;

import com.google.common.collect.Lists;
import java.util.List;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import models.base.PeriodModel;
import play.data.validation.Required;

public class MealTicketCard extends PeriodModel{
  
  public int number;

  @ManyToOne
  @Required
  public Person person;
  
  @OneToMany(mappedBy = "mealTicketCard")
  public List<MealTicket> mealTickets = Lists.newArrayList();
}
