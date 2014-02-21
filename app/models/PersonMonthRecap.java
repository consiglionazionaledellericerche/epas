package models;

import it.cnr.iit.epas.PersonUtility;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.Transient;

import models.Stamping.WayType;
import models.personalMonthSituation.CalcoloSituazioneAnnualePersona;
import models.personalMonthSituation.Mese;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;

import play.Logger;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.db.jpa.JPA;
import play.db.jpa.JPAPlugin;
import play.db.jpa.Model;

/**
 * @author cristian
 *
 */
//@Audited
@Table(name="person_months")
@Entity
public class PersonMonthRecap extends Model {

	@Required
	@ManyToOne(optional = false)
	@JoinColumn(name = "person_id", nullable = false, updatable = false)
	public Person person;	

	
	public Integer year;
	
	public Integer month;

	@Column(name="training_hours")
	public Integer trainingHours;
	
	@Column(name="hours_approved")
	public Boolean hoursApproved;

	/**
	 * aggiunta la date per test di getMaximumCoupleOfStampings ---da eliminare
	 * @param person
	 * @param year
	 * @param month
	 */
	public PersonMonthRecap(Person person, int year, int month){
		this.person = person;	
		this.year = year;
		this.month = month;

	}


	
	public static PersonMonthRecap build(Person person, int year, int month){
		PersonMonthRecap pm = PersonMonthRecap.find("Select pm from PersonMonth pm where pm.person = ? and pm.month = ? and pm.year = ?", 
				person, month, year).first();
		if(pm == null){
			pm = new PersonMonthRecap(person, year, month);
			pm.create();
		}
		
		//pm.aggiornaRiepiloghi();
		return pm;

	}

	
	public static PersonMonthRecap getInstance(Person person, int year, int month) {
		PersonMonthRecap personMonth = PersonMonthRecap.find("Select pm from PersonMonth pm where pm.person = ? and pm.year = ? and pm.month = ?", person, year, month).first();
		if (personMonth == null) {
			personMonth = new PersonMonthRecap(person, year, month);
		}
		return personMonth;
	}



		
	/**
	 * La lista delle assenze restituite è prelevata in FETCH JOIN con le absenceType i personDay e la person 
	 * in modo da non effettuare ulteriori select.
	 * 
	 * @return la lista delle assenze che non sono di tipo internalUse effettuate in questo mese dalla persona relativa
	 * 	a questo personMonth.
	 * 
	 */
	public List<Absence> getAbsencesNotInternalUseInMonth() {
		return Absence.find(
				"SELECT abs from Absence abs JOIN FETCH abs.absenceType abt JOIN FETCH abs.personDay pd JOIN FETCH pd.person p "
					+ "WHERE p = ? AND pd.date BETWEEN ? AND ? AND abt.internalUse = false ORDER BY abt.code, pd.date, abs.id", 
					person, new LocalDate(year,month, 1), new LocalDate(year, month, 1).dayOfMonth().withMaximumValue()).fetch();
	}
	
	/**
	 * metodo di utilità per il controller UploadSituation
	 * @return la lista delle competenze del dipendente in questione per quel mese in quell'anno
	 */
	public List<Competence> getCompetenceInMonthForUploadSituation(){
		List<Competence> competenceList = Competence.find("Select comp from Competence comp where comp.person = ? and comp.month = ? " +
				"and comp.year = ? and comp.valueApproved > 0", person, month, year).fetch();
		Logger.trace("Per la persona %s %s trovate %d competenze approvate nei mesi di %d/%d", person.surname, person.name, competenceList.size(), month, year );
		return competenceList;
	}
	
}
