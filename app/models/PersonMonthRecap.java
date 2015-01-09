package models;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import models.base.BaseModel;

import org.hibernate.annotations.Type;
import org.joda.time.LocalDate;

import com.google.common.base.Optional;

import dao.AbsenceDao;
import dao.CompetenceDao;
import dao.PersonMonthRecapDao;
import play.Logger;
import play.data.validation.Required;


/**
 * @author cristian
 *
 */
//@Audited
@Table(name="person_months_recap")
@Entity
public class PersonMonthRecap extends BaseModel {

	private static final long serialVersionUID = -8423858325056981355L;

	@Required
	@ManyToOne(optional = false)
	@JoinColumn(name = "person_id", nullable = false, updatable = false)
	public Person person;	

	
	public Integer year;
	
	public Integer month;
	
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	public LocalDate fromDate;
	
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	public LocalDate toDate;

	@Column(name="training_hours")
	public Integer trainingHours;
	
	@Column(name="hours_approved")
	public Boolean hoursApproved;

	/**
	 * aggiunta la date per test di getMaximumCoupleOfStampings ---da eliminarefromDate
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
		PersonMonthRecap pmr = null;
		Optional<PersonMonthRecap> pm = PersonMonthRecapDao.getPersonMonthRecapByPersonYearAndMonth(person, year, month);
//		PersonMonthRecap pm = PersonMonthRecap.find("Select pm from PersonMonth pm where pm.person = ? and pm.month = ? and pm.year = ?", 
//				person, month, year).first();
		if(!pm.isPresent()){
			pmr = new PersonMonthRecap(person, year, month);
			pmr.create();			
		}
		else
			pmr = pm.get();
		//pm.aggiornaRiepiloghi();
		return pmr;

	}

	
	public static PersonMonthRecap getInstance(Person person, int year, int month) {
		PersonMonthRecap pmr = null;
		Optional<PersonMonthRecap> personMonth = PersonMonthRecapDao.getPersonMonthRecapByPersonYearAndMonth(person, year, month);
		//PersonMonthRecap personMonth = PersonMonthRecap.find("Select pm from PersonMonth pm where pm.person = ? and pm.year = ? and pm.month = ?", person, year, month).first();
		if (!personMonth.isPresent()) {
			pmr = new PersonMonthRecap(person, year, month);
		}
		else
			pmr = personMonth.get();
		return pmr;
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
		return AbsenceDao.getAbsenceWithNotInternalUseInMonth(person, new LocalDate(year,month, 1), new LocalDate(year, month, 1).dayOfMonth().withMaximumValue());
//		return Absence.find(
//				"SELECT abs from Absence abs JOIN FETCH abs.absenceType abt JOIN FETCH abs.personDay pd JOIN FETCH pd.person p "
//					+ "WHERE p = ? AND pd.date BETWEEN ? AND ? AND abt.internalUse = false ORDER BY abt.code, pd.date, abs.id", 
//					person, new LocalDate(year,month, 1), new LocalDate(year, month, 1).dayOfMonth().withMaximumValue()).fetch();
	}
	
	/**
	 * metodo di utilità per il controller UploadSituation
	 * @return la lista delle competenze del dipendente in questione per quel mese in quell'anno
	 */
	public List<Competence> getCompetenceInMonthForUploadSituation(){
		List<Competence> competenceList = CompetenceDao.getAllCompetenceForPerson(person, year, month);
//		List<Competence> competenceList = Competence.find("Select comp from Competence comp where comp.person = ? and comp.month = ? " +
//				"and comp.year = ? and comp.valueApproved > 0", person, month, year).fetch();
		Logger.trace("Per la persona %s %s trovate %d competenze approvate nei mesi di %d/%d", person.surname, person.name, competenceList.size(), month, year );
		return competenceList;
	}
	
}
