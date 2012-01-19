package models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

import play.db.jpa.Model;

/**
 * 
 * @author dario
 *
 */
@Audited
@Entity
@Table(name = "absences")
public class Absence extends Model {
	
	public Date date;
	
	@ManyToOne
	@JoinColumn(name = "absenceType_id")
	public AbsenceType absenceType;
	
	@ManyToOne
	@JoinColumn(name="person_id")
	public Person person;
	
	/**
	 * 
	 * @param id
	 * @return absenceList
	 * funzione che ritorna l'insieme delle assenze mensili di una persona
	 */
	public List<Absence> returnAbsencesForMonth(long id){
		List<Absence> absenceList = new ArrayList<Absence>();
		
		return absenceList;
		
	}
}
