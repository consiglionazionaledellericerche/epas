package models;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDateTime;
import it.cnr.iit.epas.NullStringBinder;
import models.Stamping.WayType;
import models.base.BaseModel;
import play.data.binding.As;
import play.data.validation.Required;
import play.db.jpa.Model;

@Audited
@Entity
@Table(name = "telework_stampings")
public class TeleworkStamping extends BaseModel {

  private static final long serialVersionUID = 1L;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(nullable = false, updatable = false)
  public PersonDay personDay;
  
  @Required @NotNull
  @Column(nullable = false)
  public LocalDateTime date;
  
  @Required @NotNull
  @Enumerated(EnumType.STRING)
  public WayType way;

  @As(binder = NullStringBinder.class)
  public String note;
  
  
}
