package models;

import javax.persistence.Column;
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
@Table(name= "initialization_absences")
public class InitializationAbsence extends Model{
	
	@Required
	@ManyToOne(optional = false)
	@JoinColumn(name = "person_id", nullable = false)
	public Person person;
	
	
	@ManyToOne(optional = false)
	@JoinColumn(name= "absenceType_id", nullable = false)
	public AbsenceType absenceType;
	
	/**
	 * data da cui far partire l'inizializzazione giorni
	 */
	@Required
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	public LocalDate date;	
	
	/**
	 * giorni di ferie, ferie arretrate, permesso da inizializzare
	 */
	@Required
	@Column(name = "absenceDays")
	public int absenceDays;
	
	/**
	 * giorni di recupero avanzati dall'anno precedente
	 */
	@Column(name = "recovery_days")
	public int recoveryDays;

	
	
}
