package models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
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
import models.enumerate.JustifiedTimeAtWork;
import net.sf.oval.constraint.NotNull;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;

import play.data.validation.Required;
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
	
	@ManyToOne
	@JoinColumn(name="absence_type_group_id")
	public AbsenceTypeGroup absenceTypeGroup;
	
	@ManyToMany(fetch = FetchType.LAZY)
	public List<Qualification> qualifications = new ArrayList<Qualification>();
	
	@OneToMany(mappedBy= "absenceType", fetch = FetchType.LAZY)
	public List<InitializationAbsence> initializationAbsences = new ArrayList<InitializationAbsence>();
	
	@Required
	public String code;
	
	@Column(name = "certification_code")
	public String certificateCode;
	
	public String description;
	
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	@Column(name = "valid_from")
	public LocalDate validFrom;
	
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	@Column(name = "valid_to")
	public LocalDate validTo;
	
	@Column(name = "internal_use")
	public boolean internalUse = false;
	
	@Column(name = "multiple_use")
	public boolean multipleUse = false;

	@Column(name = "meal_ticket_calculation")
	public boolean mealTicketCalculation = false;

	@Column(name = "ignore_stamping")
	public boolean ignoreStamping = false;
	
	@Column(name = "considered_week_end")
	public boolean consideredWeekEnd = false;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "justified_time_at_work")
	public JustifiedTimeAtWork justifiedTimeAtWork;
	
	/**
	 * Se è true è un riposo compensativo che viene trattato in maniera "speciale" 
	 * rispetto agli altri tipi di assenza
	 */
	@Column(name = "compensatory_rest")
	public boolean compensatoryRest = false;
	
	/**
	 * questo campo booleano serve nei casi in cui il codice sostitutivo da usare non debba essere considerato nel calcolo dell'orario di lavoro
	 * giornaliero, ma che mi ricordi che arrivati a quel giorno, la quantità di assenze orarie per quel tipo ha superato il limite per cui deve
	 * essere inviata a Roma.
	 * Es.: i codici 09hX hanno un limite di 432 minuti che, una volta raggiunto, fa sì che a Roma debba essere inviata una assenza di tipo 09B.
	 * Questa assenza 09B viene inserita nel giorno in cui si raggiunge il limite, ma non influisce sul calcolo del tempo di lavoro di quel
	 * giorno.
	 */
	@Column(name = "replacing_absence")
	public boolean replacingAbsence = false; //FIXME inutile????
	
	@Transient
	public String getShortDescription(){
		if(description != null && description.length() > 60)
			return description.substring(0, 60)+"...";
		return description;
	}
		
	public List<Qualification> getQualification(AbsenceType abt){
		List<Qualification> listQualification = Qualification.find("Select q from Qualification q where q.absenceType = ?", abt).fetch();
		
		return listQualification;
	}
	
	public static AbsenceType getAbsenceTypeByCode(String code)
	{
		AbsenceType ab = AbsenceType.find("Select ab from AbsenceType ab where ab.code = ?", code).first();
		if(ab==null)
			return null;
		else
			return ab;
		
	}
}
