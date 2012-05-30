package models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import lombok.ToString;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.envers.query.AuditQuery;

import play.data.validation.Required;
import play.data.validation.Unique;
import play.db.jpa.JPA;
import play.db.jpa.Model;

@Entity
@Audited
@Table(name = "vacation_periods")
public class VacationPeriod extends Model{

	private static final long serialVersionUID = 7082224747753675170L;

	@Required
	@ManyToOne(cascade={})
	@JoinColumn(name="vacation_codes_id", nullable=false)
	public VacationCode vacationCode;
	
	@Unique
	@Required
	@OneToOne
	@JoinColumn(name="person_id", unique=true, nullable=false, updatable=false)
	public Person person;
	
	@Column(name="begin_from")
	public Date beginFrom;
	
	@Column(name="end_to")
	public Date endTo;
	
	/**
	 * 
	 * @param id
	 * @return vacationList
	 * funzione che ritorna una lista di ferie mensili relativi a una certa persona
	 */
	public List<VacationPeriod> vacationForMonth(long id){
		List<VacationPeriod> vacationList = new ArrayList<VacationPeriod>();
		return vacationList;
	}
	
	
	
}
