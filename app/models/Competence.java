package models;

import models.base.BaseModel;
import play.data.validation.Required;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.math.BigDecimal;


/**
 * tabella delle competenze relative alla persona in cui sono memorizzate le competenze in determinate date (espresse
 * attraverso due interi, uno relativo all'anno e uno relativo al mese con relative descrizioni e valori
 * 
 * @author dario
 * @author arianna
 *
 */
@Entity
@Table(name = "competences")
public class Competence extends BaseModel {
	
	private static final long serialVersionUID = -36737525666037452L;

	@ManyToOne
	@JoinColumn(name="person_id")
	public Person person;
	
	@Required
	@ManyToOne
	@JoinColumn(name="competence_code_id", nullable=false)
	public CompetenceCode competenceCode;
	
	public int year;
	
	public int month;	
	
	public BigDecimal valueRequested = BigDecimal.ZERO;
	
	@Column(name="exceeded_mins")
	public Integer exceededMins;
	
	public int valueApproved;
	

	
	public String reason;
	
	@Override
	public String toString() {
		return String.format("Competence[%d] - person.id = %d, competenceCode.id = %d, year = %d, month = %d, valueApproved = %d, valueRequested = %s, reason = %s",
			id, person.id, competenceCode.id, year, month, valueApproved, valueRequested, reason);
	}

	public Competence(Person person, CompetenceCode competenceCode, int year, int month) {
		super();
		this.person = person;
		this.competenceCode = competenceCode;
		this.year = year;
		this.month = month;
	}
	
	public Competence(Person person, CompetenceCode competenceCode, int year, int month, int value, String reason) {
		super();
		this.person = person;
		this.competenceCode = competenceCode;
		this.year = year;
		this.month = month;
		this.valueApproved = value;
		this.reason = reason;
	}
	

	public Competence() {
		super();
	}
	
	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}

	public int getMonth() {
		return month;
	}

	public void setMonth(int month) {
		this.month = month;
	}

	public Person getPerson() {
		return this.person;
	}
	
	public int getValueApproved() {
		return valueApproved;
	}

	public void setValueApproved(int valueApproved) {
		this.valueApproved = valueApproved;
	}
	
	public void setValueApproved(int valueApproved, String reason) {
		this.valueApproved = valueApproved;
		this.reason = reason;
	}

	public Integer getExceededMin() {
		return exceededMins;
	}

	public void setExceededMin(Integer min) {
		this.exceededMins = min;
	}
	
	public BigDecimal getValueRequested() {
		return valueRequested;
	}

	public String getReason() {
		return reason;
	}
	
	public void setValueRequested(BigDecimal valueRequested) {
		this.valueRequested = valueRequested;
	}


	
	
}
