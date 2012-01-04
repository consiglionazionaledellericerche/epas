package models;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import org.hibernate.envers.Audited;

import play.db.jpa.Model;

@Entity
@Audited
public class VacationPeriod extends Model{
	
	@ManyToOne
	@JoinColumn(name="vacation_codes_id")
	public VacationCode vacationCode;
	
	@OneToOne
	@JoinColumn(name="person_id")
	public Person person;
	
	public Date beginFrom;
	
	public Date endsTo;
	
}
