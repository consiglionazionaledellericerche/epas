package models;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import models.base.PeriodModel;

@Entity
@Table(name = "person_shift")
public class PersonShift extends PeriodModel {

  private static final long serialVersionUID = 651448817233184716L;

  public String description;

  @ManyToOne
  @JoinColumn(name = "person_id", nullable = false, updatable = false)
  public Person person;

  @OneToMany(mappedBy = "personShift")
  public List<PersonShiftShiftType> personShiftShiftTypes;

  @OneToMany(mappedBy = "personShift")
  public List<PersonShiftDay> personShiftDays = new ArrayList<>();

  public boolean disabled;

  @Override
  public String toString() {
    return person.name + " " + person.surname;
  }

}
