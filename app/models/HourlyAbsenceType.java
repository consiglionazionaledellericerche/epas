package models;

import java.util.Date;

import javax.jws.HandlerChain;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import org.hibernate.envers.Audited;

import play.db.jpa.Model;

/**
 * questa classe estende absenceType ereditandone i campi e soprattutto ereditandone l'estensione del Model
 * inoltre definisce quelle assenze di ordine orario
 * 
 * @author dario
 *
 */
@Entity
public class HourlyAbsenceType extends AbsenceType{
	
	@OneToOne
	@JoinColumn(name="absenceType_id")
	public AbsenceType absenceType;
	
	public int justifiedWorkTime;	

	public boolean mealTicketCalculation;

	public boolean ignoreStamping;	

}
