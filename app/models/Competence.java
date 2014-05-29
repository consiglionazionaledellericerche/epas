package models;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import models.base.BaseModel;
import play.data.validation.Required;


/**
 * tabella delle competenze relative alla persona in cui sono memorizzate le competenze in determinate date (espresse
 * attraverso due interi, uno relativo all'anno e uno relativo al mese con relative descrizioni e valori
 * 
 * @author dario
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
	
	public int valueApproved;
	
	public int valueRequest;
	
	public String reason;
	
	@Override
	public String toString() {
		return String.format("Competence[%d] - person.id = %d, competenceCode.id = %d, year = %d, month = %d,value = %d",
			id, person.id, competenceCode.id, year, month, valueApproved);
	}

	public Competence(Person person, CompetenceCode competenceCode, int year, int month) {
		super();
		this.person = person;
		this.competenceCode = competenceCode;
		this.year = year;
		this.month = month;
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

	public int getValueApproved() {
		return valueApproved;
	}

	public void setValueApproved(int valueApproved, String reason) {
		this.valueApproved = valueApproved;
		this.reason = reason;
	}

	public int getValueRequest() {
		return valueRequest;
	}

	public String getReason() {
		return reason;
	}
	
	public void setRequest(int valueRequest) {
		this.valueRequest = valueRequest;
	}


	
	
}
