package models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import models.enumerate.AccumulationBehaviour;
import models.enumerate.AccumulationType;

import org.hibernate.envers.Audited;

import play.db.jpa.Model;
/**
 * 
 * @author dario
 *
 */
@Entity
@Table(name="absence_types")
@Audited
public class AbsenceType extends Model {
	
	private static final long serialVersionUID = 7157167508454574329L;

	@OneToMany(mappedBy="absenceType")
	public List<Absence> absences;	
	
	@ManyToOne
	@JoinColumn(name="absence_type_group_id")
	public AbsenceTypeGroup absenceTypeGroup;
	
	@ManyToMany(cascade = {CascadeType.REFRESH}, fetch = FetchType.LAZY)
	public List<Qualification> qualifications;
	
	public String code;
	
	public String certificateCode;
	
	public String description;
	
	public Date validFrom;
	
	public Date validTo;
	
	public boolean internalUse;
	
	public boolean multipleUse;	

	public boolean mealTicketCalculation;

	public boolean ignoreStamping;		
	
	public boolean isHourlyAbsence;
	
	public int justifiedWorkTime;
	
	public boolean isDailyAbsence;

	
	/**
	 * questo campo booleano serve nei casi in cui il codice sostitutivo da usare non debba essere considerato nel calcolo dell'orario di lavoro
	 * giornaliero, ma che mi ricordi che arrivati a quel giorno, la quantità di assenze orarie per quel tipo ha superato il limite per cui deve
	 * essere inviata a Roma.
	 * Es.: i codici 09hX hanno un limite di 432 minuti che, una volta raggiunto, fa sì che a Roma debba essere inviata una assenza di tipo 09B.
	 * Questa assenza 09B viene inserita nel giorno in cui si raggiunge il limite, ma non influisce sul calcolo del tempo di lavoro di quel
	 * giorno.
	 */
	public boolean replacingAbsence = false;
	
	@Transient
	public String getShortDescription(){
		if(description != null && description.length() > 60)
			return description.substring(0, 60)+"...";
		return description;
	}
		
}
