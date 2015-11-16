/**
 * 
 */
package models.exports;

import models.Person;
import models.PersonReperibilityType;
import org.joda.time.LocalDate;

/**
 * Classe di supporto per l'esportazione delle informazioni relative
 * alla reperibilità delle persone.
 * 
 * @author cristian
 *
 */
public class ReperibilityPeriod {

	public final Person person;
	public final LocalDate start;
	public LocalDate end;
	public PersonReperibilityType reperibilityType;

	public ReperibilityPeriod(Person person, LocalDate start, LocalDate end) {
		this.person = person;
		this.start = start;
		this.end = end;
	}

	public ReperibilityPeriod(Person person, LocalDate start, LocalDate end, PersonReperibilityType type) {
		this.person = person;
		this.start = start;
		this.end = end;
		this.reperibilityType = type;
	}
}
