package models;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import models.base.BaseModel;
import models.enumerate.JustifiedTimeAtWork;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;

import play.data.validation.Required;

import com.google.common.collect.Sets;
/**
 *
 * @author dario
 *
 */
@Entity
@Table(name="absence_types")
@Audited
public class AbsenceType extends BaseModel {

	private static final long serialVersionUID = 7157167508454574329L;

	@ManyToOne(fetch = FetchType.LAZY)
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


	@Column(name = "valid_from")
	public LocalDate validFrom;


	@Column(name = "valid_to")
	public LocalDate validTo;

	@Column(name = "internal_use")
	public boolean internalUse = false;

	@Column(name = "multiple_use")
	public boolean multipleUse = false;
	//FIXME questo campo non è mai utilizzato. la sua utilità mi sfugge


	@Column(name = "meal_ticket_calculation")
	public boolean mealTicketCalculation = false;
	//FIXME questo campo non e' mai utilizzato, e' il caso della missione che prevede comunque il calcolo del buono mensa?


	@Column(name = "considered_week_end")
	public boolean consideredWeekEnd = false;

	@Enumerated(EnumType.STRING)
	@Column(name = "justified_time_at_work")
	public JustifiedTimeAtWork justifiedTimeAtWork;

	/**
	 * questo campo booleano serve nei casi in cui il codice sostitutivo da usare non debba essere considerato nel calcolo dell'orario di lavoro
	 * giornaliero, ma che mi ricordi che arrivati a quel giorno, la quantità di assenze orarie per quel tipo ha superato il limite per cui deve
	 * essere inviata a Roma.
	 * Es.: i codici 09hX hanno un limite di 432 minuti che, una volta raggiunto, fa sì che a Roma debba essere inviata una assenza di tipo 09B.
	 * Questa assenza 09B viene inserita nel giorno in cui si raggiunge il limite, ma non influisce sul calcolo del tempo di lavoro di quel
	 * giorno.
	 */
	@Column(name = "replacing_absence")
	public boolean replacingAbsence = false;
	//FIXME questo campo non è mai utilizzato

	/**
	 * Relazione inversa con le assenze.
	 */
	@OneToMany(mappedBy="absenceType")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public Set<Absence> absences = Sets.newHashSet();


	@Transient
	public String getShortDescription(){
		if(description != null && description.length() > 60)
			return description.substring(0, 60)+"...";
		return description;
	}

}
