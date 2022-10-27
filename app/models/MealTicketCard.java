package models;

import com.google.common.collect.Lists;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import models.absences.InitializationGroup;
import models.base.PeriodModel;
import models.flows.Affiliation;
import models.flows.Group;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;
import play.data.validation.Required;

/**
 * Nuova classe che implementa le card dei buoni elettronici.
 *
 * @author dario
 *
 */
@Getter
@Setter
@Entity
@Audited
@Table(name = "meal_ticket_card")
public class MealTicketCard extends PeriodModel {
  
  private int number;

  @ManyToOne
  @Required
  private Person person;
  
  @OneToMany(mappedBy = "mealTicketCard")
  private List<MealTicket> mealTickets = Lists.newArrayList();
}
