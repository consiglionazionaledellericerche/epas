package models;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import models.base.BaseModel;

import org.hibernate.envers.NotAudited;


@Entity
@Table(name="person_days_in_trouble")
public class PersonDayInTrouble extends BaseModel
{
	
	public final static String UNCOUPLED_FIXED = "timbratura disaccoppiata persona fixed";
	public final static String NO_ABS_NO_STAMP = "no assenze giornaliere e no timbrature";
	public final static String UNCOUPLED_WORKING = "timbratura disaccoppiata giorno feriale";
	public final static String UNCOUPLED_HOLIDAY = "timbratura disaccoppiata giorno festivo";
	
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
