package models.flows;

import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import org.apache.commons.compress.utils.Lists;
import org.hibernate.envers.Audited;
import models.Person;
import models.base.MutableModel;
import play.data.validation.Required;

@Audited
@Entity
@Table(name="groups")
public class Group extends MutableModel {

  public String name;
  
  public String description;
  
  @Column(name="send_flows_email")
  public boolean sendFlowsEmail = false;
  
  @ManyToOne
  @JoinColumn(name = "manager", nullable = false)
  @Required
  public Person manager;
  
  @ManyToMany
  public List<Person> people = Lists.newArrayList();
}
