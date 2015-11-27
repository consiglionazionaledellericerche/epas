package models;

import com.google.common.base.MoreObjects;

import models.base.BaseModel;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;

import play.data.validation.Required;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

@Audited
@Entity
@Table(name = "meal_ticket")
public class MealTicket extends BaseModel {

  private static final long serialVersionUID = -963204680918650598L;

  @NotAudited
  @Required
  @ManyToOne(optional = false)
  @JoinColumn(name = "contract_id", nullable = false)
  public Contract contract;

  public Integer year;

  public Integer quarter;

  @Required
  public LocalDate date;

  @Required
  public Integer block;	/*esempio 5941 3165 01 */

  public Integer number;

  public String code; /* concatenzazione block + number */

  @Required
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "admin_id", nullable = false)
  public Person admin;

  @Required
  @Column(name = "expire_date")

  public LocalDate expireDate;

  @Transient
  public Boolean used = null;

  @Override
  public String toString() {

    return MoreObjects.toStringHelper(this)
            .add("id", id)
            .add("contract", contract.id)
            .add("person", contract.person.name + " " + contract.person.surname)
            .add("date", date)
            .add("expire", expireDate).toString();

  }
}
