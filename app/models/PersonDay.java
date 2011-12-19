/**
 * 
 */
package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;

import play.Logger;
import play.db.jpa.JPA;

import lombok.Data;

/**
 * Classe che rappresenta un giorno, sia esso lavorativo o festivo di una persona.
 *  
 * @author cristian
 *
 */
@Data
public class PersonDay {

	public final LocalDate date;
	
	private final Person person;
	
	private List<Stamping> stampings = null;
	
	private Absence absence = null;
	
	/**
	 * Totale del tempo lavorato nel giorno in minuti 
	 */
	private Integer dailyTime;
	
	public PersonDay(Person person, LocalDate date) {
		this.person = person;
		this.date = date;
	}
	
	/**	 
	 * Calcola se un giorno è lavorativo o meno. L'informazione viene calcolata a partire
	 * dal giorno e dal WorkingTimeType corrente della persona
	 * 
	 * @return true se il giorno corrente è un giorno lavorativo, false altrimenti.
	 */
	public boolean isWorkingDay() {
		EntityManager em = JPA.em();
		WorkingTimeTypeDay wttd = em.createQuery("SELECT wttd FROM Person p JOIN p.workingTimeType wtt JOIN wtt.workingTimeTypeDay wttd " + 
				"WHERE p.id = :personId AND wttd.dayOfWeek = :day", WorkingTimeTypeDay.class)
				.setParameter("personId", person.id)
				.setParameter("day", date.getDayOfWeek())
				.getSingleResult();
		return !wttd.holiday;
	}
	
	public boolean isAbsent() {
		if (getAbsence() != null) {
			if (!stampings.isEmpty()) {
				Logger.warn("Attenzione il giorno %s è presente un'assenza (%s) (Person = %s), però ci sono anche %d timbrature.", 
					absence, absence.person, stampings.size());
			}
		}
		return absence != null;
	}
	
	public Absence getAbsence() {
		if (absence == null) {
			//si calcola prendendola dal db
		}
		return absence;
	}
	
	public List<Stamping> getStampings() {
		if (stampings == null) {
			EntityManager em = JPA.em();

			stampings = 
				em.createQuery("SELECT s FROM Stamping WHERE s.person = :person and date >= :startDate and date < endDate ORDER BY date", 
					Stamping.class)
				.setParameter("person", person)
				.setParameter("startDate", date.toDateMidnight())
				.setParameter("endDate", date.plusDays(1).toDateMidnight())
				.getResultList();
		}
		return stampings;
	}
	
}
