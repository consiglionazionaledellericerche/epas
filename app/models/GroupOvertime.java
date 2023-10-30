package models;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import models.base.BaseModel;
import models.flows.Group;
import play.data.validation.Required;

/**
 * Oggetto che modella il calcolo totale degli straordinari per gruppo.
 *
 * @author Dario Tagliaferri
 */
@Getter
@Setter
@Entity
@Audited
@Table(name = "group_overtimes")
public class GroupOvertime extends BaseModel{

  @NotNull
  private LocalDate dateOfUpdate;

  private Integer numberOfHours;

  private Integer year;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "group_id")
  private Group group;
}
