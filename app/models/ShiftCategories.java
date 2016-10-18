package models;

import models.base.BaseModel;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;


@Entity
@Audited
@Table(name = "shift_categories")
public class ShiftCategories extends BaseModel {

  private static final long serialVersionUID = 3156856871540530483L;

  public String description;

  /**
   * responsabile della categoria turno.
   */
  @ManyToOne(optional = false)
  @JoinColumn(name = "supervisor")
  public Person supervisor;
}

