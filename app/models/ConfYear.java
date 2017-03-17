package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import models.base.BaseModel;

import org.hibernate.envers.Audited;

/**
 * Da eliminare quando è stata applicata la migrazione in tutte le installazioni.
 * @author alessandro
 *
 */
@Deprecated
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

    Integer value = Integer.parseInt(this.fieldValue);

    if (value == 0) {
      return "nessun limite";
    }
    if (value == 1) {
      return "entro fine gennaio";
    }
    if (value == 2) {
      return "entro fine febbraio";
    }
    if (value == 3) {
      return "entro fine marzo";
    }
    if (value == 4) {
      return "entro fine aprile";
    }
    if (value == 5) {
      return "entro fine maggio";
    }
    if (value == 6) {
      return "entro fine giugno";
    }
    if (value == 7) {
      return "entro fine luglio";
    }
    if (value == 8) {
      return "entro fine agosto";
    }
    if (value == 9) {
      return "entro fine settembre";
    }
    if (value == 10) {
      return "entro fine ottobre";
    }
    if (value == 11) {
      return "entro fine novembre";
    }
    if (value == 12) {
      return "entro fine dicembre";
    }
    return null;

  }


  public String getIntelligibleNumberValue() {
    Integer value = Integer.parseInt(this.fieldValue);

    if (value == 0) {
      return "nessun limite";
    } else {
      return value + "";
    }
  }

}
