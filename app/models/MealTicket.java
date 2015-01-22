package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import models.base.BaseModel;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;

import com.google.common.base.Objects;

import play.data.validation.Required;

@Audited
@Entity
@Table(name = "meal_ticket")
public class MealTicket extends BaseModel{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@NotAudited
	@Required
	@ManyToOne(optional = false)
	@JoinColumn(name = "contract_id", nullable = false)
	public Contract contract;
	
	public Integer year;
	
	public Integer quarter;
	
	@Required
	//  @Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	public LocalDate date;
	
	@Required
	public Integer block;	/*esempio 5941 3165 01 */
	
	public Integer number;
	
	public String code; /* concatenzazione block + number */
	
	@Required
	@ManyToOne(optional = false)
	@JoinColumn(name = "admin_id", nullable = false)
	public Person admin;
	
	@Required
	@Column(name = "expire_date")
	//  @Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	public LocalDate expireDate;
	
	@Transient 
	public Boolean used = null;
	
	@Override
	public String toString() {
		
		return Objects.toStringHelper(this)
				.add("id", id)
				.add("contract", contract.id)
				.add("person", contract.person.name + " " + contract.person.surname)
				.add("date", date)
				.add("expire", expireDate).toString();
		
	}
	

}
