package models.flows;

import com.google.common.collect.Lists;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import models.Office;
import models.Person;
import models.base.MutableModel;
import org.hibernate.envers.Audited;
import play.data.validation.Required;

@Audited
@Entity
@Table(name = "groups")
public class Group extends MutableModel {

  private static final long serialVersionUID = -5169540784395404L;

  public String name;

  public String description;

  @Column(name = "send_flows_email")
  public boolean sendFlowsEmail;

  @ManyToOne
  @JoinColumn(name = "office_id", nullable = false)
  public Office office;
  
  @ManyToOne
  @JoinColumn(name = "manager", nullable = false)
  @Required
  public Person manager;

  @ManyToMany
  public List<Person> people = Lists.newArrayList();
}
