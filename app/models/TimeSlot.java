package models;

import com.google.common.collect.Lists;
import helpers.LocalTimeBinder;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import models.base.BaseModel;
import org.assertj.core.util.Strings;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import play.data.binding.As;
import play.data.validation.Unique;


/**
 * Modello per le fasce di orario lavorativo dei dipendenti.
 * Utilizzato quando necessario per le fasce di orario obbligatorie.
 *
 * @author cristian
 */
@Entity
@Audited
@Table(name = "time_slots")
public class TimeSlot extends BaseModel {

  private static final long serialVersionUID = -3443521979786226461L;

  @ManyToOne
  @JoinColumn(name = "office_id")
  public Office office;

  @Getter
  @Column
  @Unique("office,beginSlot,endSlot")
  public String description;
  
  @Unique("office,beginSlot,endSlot")
  @As(binder = LocalTimeBinder.class)
  @NotNull
  @Column(columnDefinition = "VARCHAR")
  public LocalTime beginSlot;
  
  @As(binder = LocalTimeBinder.class)
  @NotNull
  @Column(columnDefinition = "VARCHAR")
  public LocalTime endSlot;
  
  @NotAudited
  @OneToMany(mappedBy = "timeSlot")
  public List<ContractMandatoryTimeSlot> contractMandatoryTimeSlots = Lists.newArrayList();

  @Column(name = "disabled")
  public boolean disabled = false;

  @Transient
  public String getLabel() {
    DateTimeFormatter dtf = DateTimeFormat.forPattern("HH:mm");
    return Strings.isNullOrEmpty(description) ?  
        String.format("%s - %s", dtf.print(beginSlot), dtf.print(endSlot)) 
          : 
        String.format("%s (%s - %s)", description, dtf.print(beginSlot), dtf.print(endSlot));
  }

  @Override
  public String toString() {
    return description;
  }

}