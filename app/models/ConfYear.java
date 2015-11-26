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
@Table(name = "conf_year")
public class ConfYear extends BaseModel {

  private static final long serialVersionUID = -3157754270960969163L;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "office_id")
  public Office office;

  @Column(name = "year")
  public Integer year;

  @Column(name = "field")
  public String field;

  @Column(name = "field_value")
  public String fieldValue;

  public ConfYear() {
    this.year = null;
    this.office = null;
    this.field = null;
    this.fieldValue = null;
  }

  public ConfYear(Office office, Integer year, String fieldName, String fieldValue) {
    this.office = office;
    this.year = year;
    this.field = fieldName;
    this.fieldValue = fieldValue;
  }


  public String getIntelligibleMonthValue() {

    Integer i = Integer.parseInt(this.fieldValue);

    if (i == 0)
      return "nessun limite";
    if (i == 1)
      return "entro fine gennaio";
    if (i == 2)
      return "entro fine febbraio";
    if (i == 3)
      return "entro fine marzo";
    if (i == 4)
      return "entro fine aprile";
    if (i == 5)
      return "entro fine maggio";
    if (i == 6)
      return "entro fine giugno";
    if (i == 7)
      return "entro fine luglio";
    if (i == 8)
      return "entro fine agosto";
    if (i == 9)
      return "entro fine settembre";
    if (i == 10)
      return "entro fine ottobre";
    if (i == 11)
      return "entro fine novembre";
    if (i == 12)
      return "entro fine dicembre";
    return null;

  }


  public String getIntelligibleNumberValue() {
    Integer i = Integer.parseInt(this.fieldValue);

    if (i == 0)
      return "nessun limite";
    else
      return i + "";
  }

}
