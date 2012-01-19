package models;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

import play.db.jpa.Model;

/**
 * 
 * modificata temporaneamente per permettere la gestione del solo giorno di ferie relativo alla persona
 * 
 * @author dario
 */
@Audited
@Entity
@Table(name = "person_vacations")
public class PersonVacation extends Model{
	
	private static final long serialVersionUID = -4964482244412822954L;

	@ManyToOne	
	@JoinColumn(name = "person_id", nullable = false)
	public Person person;
	
//	@ManyToOne
//	@JoinColumn(name = "vacationType_id")
//	public VacationType vacationType;
	
//	@Column(name = "begin_from")
//	public Date beginFrom;
//	
//	@Column(name = "end_to")
//	public Date endTo;
	@Column(name="vacation_day")
	public Date vacationDay;
	
}
