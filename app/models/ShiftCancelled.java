package models;

import models.base.BaseModel;

import org.joda.time.LocalDate;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;


@Entity
@Table(name = "shift_cancelled")
public class ShiftCancelled extends BaseModel {

  private static final long serialVersionUID = -6164045507709173642L;


  public LocalDate date;

  @ManyToOne
  @JoinColumn(name = "shift_type_id", nullable = false)
  public ShiftType type;
}
