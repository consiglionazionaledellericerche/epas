package models;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import models.base.BaseModel;

import org.hibernate.envers.NotAudited;

import play.Logger;


@Entity
@Table(name="person_days_in_trouble")
public class PersonDayInTrouble extends BaseModel
{
	
	private static final long serialVersionUID = 4802468368796902865L;
	
	public String cause;
	public boolean fixed;
	public boolean emailSent;
	
	@NotAudited
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="personday_id", nullable=false, updatable=false)
	public PersonDay personDay;

	public PersonDayInTrouble(PersonDay pd, String cause)
	{
		this.personDay = pd;
		this.cause = cause;
		this.fixed = false;
		this.emailSent = false;
	}

	
}
