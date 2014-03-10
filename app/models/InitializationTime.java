package models;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;

import play.data.validation.Required;
import play.db.jpa.Model;

@Audited
@Entity
@Table(name= "initialization_times")
public class InitializationTime extends Model{

	@Required
	@ManyToOne(optional = false)
	@JoinColumn(name = "person_id", nullable = false)
	public Person person;
	
	/**
	 * data da cui far partire l'inizializzazione di minuti
	 */
	@Required
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	public LocalDate date;	
	
	/**
	 * minuti di residuo da inizializzare maturati nell'anno relativo alla data dell'ogggetto 
	 */
	@Required
	public Integer residualMinutesCurrentYear;

	/**
	 * minuti di residuo da inizializzare maturati nell'anno precedente alla data dell'ogggetto 
	 */
	@Required
	public Integer residualMinutesPastYear;

}
