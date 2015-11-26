package models;

import models.base.BaseModel;

import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;


@Audited
@Entity
@Table(name = "conf_general")
public class ConfGeneral extends BaseModel {

  private static final long serialVersionUID = 4941937973447699263L;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "office_id")
  public Office office;

  @Column(name = "field")
  public String field;

  @Column(name = "field_value")
  public String fieldValue;

  public ConfGeneral() {
    this.office = null;
    this.field = null;
    this.fieldValue = null;
  }

  public ConfGeneral(Office office, String fieldName, String fieldValue) {
    this.office = office;
    this.field = fieldName;
    this.fieldValue = fieldValue;
  }

}
