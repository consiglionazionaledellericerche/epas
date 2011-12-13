/**
 * 
 */
package models;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import org.joda.time.LocalDate;

import play.Logger;

/**
 * Classe che rappresenta un giorno, sia esso lavorativo o festivo.
 * Nel caso di giorni lavorativo contiene anche le eventuali informazioni relative alle timbrature
 * o assenze 
 * 
 * @author cristian
 *
 */
@Data
public class Day {

	public final LocalDate date;
	
	public List<Stamping> stampings = new ArrayList<Stamping>();
	
	public Absence absence;
	
	public Day(LocalDate date) {
		this.date = date;
	}
	
	public boolean isAbsent() {
		if (absence != null) {
			if (!stampings.isEmpty()) {
				Logger.warn("Attenzione il giorno %s è presente un'assenza (%s) (Person = %s), però ci sono anche %d timbrature.", 
					absence, absence.person, stampings.size());
			}
		}
		return absence != null;
	}
}
