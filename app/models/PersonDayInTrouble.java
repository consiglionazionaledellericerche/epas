package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import play.data.validation.Unique;
import play.db.jpa.Model;

@Entity
@Table(name="person_days_in_trouble")
public class PersonDayInTrouble extends Model
{
	
	public String cause;
	public boolean fixed;
	public boolean emailSent;
	
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="personday_id", unique=true, nullable=false, updatable=true)
	public PersonDay personDay;
	
	public PersonDayInTrouble()
	{
		this.fixed = false;
		this.emailSent = false;
	}
}
