package models;

import models.base.BaseModel;
import models.enumerate.Troubles;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Audited
@Entity
@Table(name="person_days_in_trouble")
public class PersonDayInTrouble extends BaseModel{

	private static final long serialVersionUID = 4802468368796902865L;
	
	@Enumerated(EnumType.STRING)
	public Troubles cause;
	
	public boolean emailSent;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="personday_id", nullable=false, updatable=false)
	public PersonDay personDay;

	public PersonDayInTrouble(PersonDay pd, Troubles cause){
		this.personDay = pd;
		this.cause = cause;
		this.emailSent = false;
	}
}
