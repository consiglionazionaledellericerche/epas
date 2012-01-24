package models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

import play.db.jpa.Model;

@Entity
@Audited
@Table(name = "vacation_periods")
public class VacationPeriod extends Model{

	private static final long serialVersionUID = 7082224747753675170L;

	@ManyToOne
	@JoinColumn(name="vacation_codes_id")
	public VacationCode vacationCode;
	
	@OneToOne
	@JoinColumn(name="person_id")
	public Person person;
	
	public Date beginFrom;
	
	public Date endsTo;
	
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
