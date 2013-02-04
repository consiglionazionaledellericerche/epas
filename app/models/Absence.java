package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;

import play.data.validation.Required;
import play.db.jpa.Blob;
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
	
	@ManyToOne
	@JoinColumn(name = "absence_type_id")
	public AbsenceType absenceType;
	
	
	@ManyToOne(optional=false)
	@JoinColumn(name="personDay_id", nullable=false)
	public PersonDay personDay;
	
	public Blob absenceRequest;

	@Override
	public String toString() {
		return String.format("Absence[%d] - personDay.id = %d, absenceType.id = %s", 
			id, personDay.id, absenceType.id);
	}
}
