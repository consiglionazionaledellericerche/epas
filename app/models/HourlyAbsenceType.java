package models;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

/**
 * questa classe estende absenceType ereditandone i campi e soprattutto ereditandone l'estensione del Model
 * inoltre definisce quelle assenze di ordine orario
 * 
 * @author dario
 *
 */
@Audited
@Entity
@Table(name = "hourly_absence_types")
@PrimaryKeyJoinColumn(name="absence_type_id")
public class HourlyAbsenceType extends AbsenceType {
	
	private static final long serialVersionUID = 6598043062600364402L;

	@OneToOne
	@JoinColumn(name="absenceType_id")
	public AbsenceType absenceType;
	
	public int justifiedWorkTime;	


}
