package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.envers.Audited;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;

import play.Logger;
import play.db.jpa.JPA;
import play.db.jpa.Model;

/**
 * 
 * @author dario
 * questa classe rappresenta il residuo annuale relativo a una certa persona per quel che riguarda le ore fatte in più (o in meno)
 * 
 */
@Audited
@Entity
@Table(name="person_years")
public class PersonYear extends Model{
	
	@ManyToOne
	@JoinColumn(name = "person_id", nullable = false)
	public Person person;
	
	@Column
	public int year;
	
	@Column(name = "remaining_vacation_days")
	public Integer remainingVacationDays;
	
	/**
	 * Tempo in minuti residuo alla fine dell'anno
	 */
	@Column(name = "remaining_minutes")
	public Integer remainingMinutes;
	

	public PersonYear(Person person, int year){
		this.person = person;
		this.year = year;
	}
	
	
	/**
	 * aggiorna le variabili di istanza in funzione dei valori presenti sul db
	 * non fa il salvataggio dei dati (speculare al refreshPersonMonth
	 */
	public void refreshPersonYear(){
		//Configuration config = Configuration.getCurrentConfiguration();
		LocalDate data = new LocalDate();
		if(data.getDayOfMonth() == 1 && data.getMonthOfYear() == DateTimeConstants.JANUARY){
			List<PersonMonth> personMonthList = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and pm.year = ?",
					person, year).fetch();
			for(PersonMonth pm : personMonthList){
				remainingMinutes = remainingMinutes + pm.totalRemainingMinutes;
			}
		}
		this.save();
	}
	
	/**
	 * conta quanti giorni di ferie sono rimasti da utilizzare dalle ferie dell'anno corrente
	 */
//	public int getRemainingVacationDays(){
//		List<Absence> absList = new ArrayList<Absence>();
//		if(remainingVacationDays == null){
//			remainingVacationDays = 0;
//			LocalDate date = new LocalDate(year, 1, 1);
//			List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?", 
//					person, date, date.plusYears(1)).fetch();
//			for(PersonDay pd : pdList){
//				if(pd.absences.size() > 0){
//					for(Absence abs : pd.absences){
//						if(abs.absenceType.code.equals("32"))
//							absList.add(abs);
//					}
//				}
//			}
//			if(person.getCurrentContract().vacationPeriod != null)
//				remainingVacationDays = person.getCurrentContract().vacationPeriod.vacationCode.vacationDays - absList.size();
//			else 
//				remainingVacationDays = 0;
//			save();
//		}
//		
//		return remainingVacationDays;
//	}
	
	/**
	 * ritorna quanti minuti sono in più/in meno alla fine dell'anno
	 */
	public int getRemainingMinutes(){
		if(remainingMinutes == null){
			remainingMinutes = 0;
			List<PersonMonth> personMonthList = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and pm.year = ?",
					person, year).fetch();
			Logger.debug("La lista dei personMonth per %s %s: %s",person.name, person.surname, personMonthList);
			if(personMonthList != null){
				for(PersonMonth pm : personMonthList){
					remainingMinutes = remainingMinutes + pm.totalRemainingMinutes;
				}
				
			}
			else
				remainingMinutes = 0;
			this.save();
		}
		return remainingMinutes;
	}
	
	
	public static PersonYear build(Person person, int year){
		PersonYear py = new PersonYear(person, year);
		py.create();
		LocalDate date = new LocalDate(year, 1, 1);
		Contract contract = person.getContract(new LocalDate(date.monthOfYear().withMaximumValue().dayOfYear().withMaximumValue()));
		
		if(contract != null){
			int vacationDaysActualYear = 0;
			PersonMonth pm = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and pm.year = ? " +
					"order by pm.month desc ", person, year).first();
			//py.remainingMinutes = pm.totaleResiduoAnnoCorrenteAFineMese();
			
			List<PersonDay> pdList = null;
			if(contract.beginContract != null && contract.expireContract != null){
				pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ? " +
						"and pd.date > ? and pd.date < ?",
						person, date, date.monthOfYear().withMaximumValue().dayOfYear().withMaximumValue(), contract.beginContract, contract.expireContract).fetch();
		 		
			}
			if(contract.beginContract != null && contract.expireContract == null){
				pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ? " +
						"and pd.date > ? and pd.date < ?",
						person, date, date.monthOfYear().withMaximumValue().dayOfYear().withMaximumValue(), contract.beginContract, new LocalDate(2099,12,31)).fetch();
			}
			else{
				pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ? ",
						person, date, date.monthOfYear().withMaximumValue().dayOfYear().withMaximumValue()).fetch();
			}
			for(PersonDay pd : pdList){
	 			if(pd.absences.size() > 0){
	 				for(Absence abs : pd.absences){
	 					if(abs.absenceType.code.equals("32"))
	 						vacationDaysActualYear = vacationDaysActualYear +1;
	 				}
	 			}
	 		}
			
//	 		VacationPeriod per = VacationPeriod.find("Select per from VacationPeriod per where per.person = ? and per.beginFrom < ? " +
//	 				"and per.endTo > ?", person, date, date.monthOfYear().withMaximumValue().dayOfYear().withMaximumValue()).first();
//	 		if(per != null)
//	 			py.remainingVacationDays = per.vacationCode.vacationDays - vacationDaysActualYear;
//	 		else 
//	 			py.remainingVacationDays = 0;
			//TODO: capire a cosa serviva questa roba

		}
				
		py.save();
	
		return py;
	}
	
		
}
