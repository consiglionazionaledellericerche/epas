package models;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import org.hibernate.envers.Audited;
import models.base.BaseModel;
/**
 * 
 * @author dario
 * Nuova relazione tra timetable delle organizzazioni e sedi.
 */
import models.enumerate.CalculationType;
import play.data.validation.Required;
@Audited
@Entity
public class OrganizationShiftTimeTable extends BaseModel{

  public String name;
  
  @OneToMany(mappedBy= "shiftTimeTable", fetch = FetchType.EAGER)
  public Set<OrganizationShiftSlot> organizationShiftSlot;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "office_id")
  public Office office;
  
  @Required
  @Enumerated(EnumType.STRING)
  @Column(name = "calculation_type")
  public CalculationType calculationType;
  
  @OneToMany(mappedBy = "shiftTimeTable")
  public List<ShiftType> shiftTypes = new ArrayList<>();
}
