package models;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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

import com.google.common.base.Joiner;
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

	@ManyToOne
	@JoinColumn(name="absence_type_group_id")
	public AbsenceTypeGroup absenceTypeGroup;

	@ManyToMany
	public List<Qualification> qualifications = new ArrayList<Qualification>();

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

	@Column(name = "considered_week_end")
	public boolean consideredWeekEnd = false;
	
	@Required
	@Enumerated(EnumType.STRING)
	@Column(name = "justified_time_at_work")
	public JustifiedTimeAtWork justifiedTimeAtWork;

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
	
	@Override
	public String toString() {
		return Joiner.on(" - ").skipNulls().join(code,description);
	}

}
