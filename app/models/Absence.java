package models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;

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
	
	private static final long serialVersionUID = -1963061850354314327L;

	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	public LocalDate date;
	
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
	public static List<Absence> returnAbsencesForMonth(long id){
		List<Absence> absenceList = new ArrayList<Absence>();
		
		return absenceList;
		
	}
}
