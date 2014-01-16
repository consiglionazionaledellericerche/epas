package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import play.Logger;
import play.data.validation.Unique;
import play.db.jpa.Model;

@Entity
//@Audited
@Table(name="person_days_in_trouble")
public class PersonDayInTrouble extends Model
{
	
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

	public static void insertPersonDayInTrouble(PersonDay pd, String cause)
	{
		if(pd.troubles==null || pd.troubles.size()==0)
		{	
			//se non esiste lo creo
			Logger.info("Nuovo PersonDayInTrouble %s %s %s - %s - %s", pd.person.id, pd.person.name, pd.person.surname, pd.date, cause);
			PersonDayInTrouble trouble = new PersonDayInTrouble(pd, cause);
			trouble.save();
			pd.troubles.add(trouble);
			pd.save();
			return;
		}
		else
		{
			//se esiste lo setto fixed = false;
			pd.troubles.get(0).fixed = false;
			pd.troubles.get(0).cause = cause;
			pd.troubles.get(0).save();
			pd.save();
		}

	}
}
