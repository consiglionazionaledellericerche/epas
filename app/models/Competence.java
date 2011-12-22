package models;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.joda.time.LocalDate;

import play.db.jpa.Model;

/**
 * tabella delle competenze relative alla persona in cui sono memorizzate le competenze in determinate date (espresse
 * attraverso due interi, uno relativo all'anno e uno relativo al mese con relative descrizioni e valori
 * @author dario
 *
 */
@Entity
public class Competence extends Model{
	
	@ManyToOne
	@JoinColumn(name="person_id")
	public Person person;
	
	@ManyToOne
	@JoinColumn(name="competence_code_id")
	public CompetenceCode competenceCode;
	
	public int year;
	
	public int month;
	
	public String code;	
	
	public int value;
	
	
}
