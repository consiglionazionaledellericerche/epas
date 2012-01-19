package models;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

/**
 * questa classe estende AbsenceType ereditandone i campi e soprattutto ereditandone l'estensione del Model
 * inoltre definisce quelle assenze di ordine giornaliero
 * 
 * @author dario
 *
 */
@Audited
@Entity
@Table(name = "dayly_absence_types")
@PrimaryKeyJoinColumn(name="absence_type_id")
public class DailyAbsenceType extends AbsenceType {	

	private static final long serialVersionUID = 6914543997511178848L;
	
	@OneToOne
	@JoinColumn(name="absenceType_id")
	public AbsenceType absenceType;


}
