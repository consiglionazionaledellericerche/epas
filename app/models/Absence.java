package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;

import play.db.jpa.Model;

/**
 * 
 * @author dario
 *
 */
@Audited
@Entity
@Table(name = "absences")
public class Absence extends Model {
	
	private static final long serialVersionUID = -1963061850354314327L;

	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	public LocalDate date;
	
	@ManyToOne
	@JoinColumn(name = "absenceType_id")
	public AbsenceType absenceType;
	
	@ManyToOne
	@JoinColumn(name="person_id")
	public Person person;
	
	/**
	 * 
	 * @return il numero di giorni di ferie maturati nell'anno corrente con l'ausilio di un metodo di codifica privato
	 */
	public int vacationCurrentYear(int year){
		int days = 0;
		int vacationDays = 0;
		Contract contract = Contract.find("Select con from Contract con where con.person = ? order by con.begincontract", person).first();
		LocalDate beginContract = new LocalDate(contract.beginContract);
		LocalDate now = new LocalDate().now();
		now.withYear(year);
		if(now.getYear()-beginContract.getYear() < 3){
			if(now.getYear()-beginContract.getYear() < 1 && beginContract.getMonthOfYear() != 1 && beginContract.getDayOfMonth() != 1){
				LocalDate newDate = now.minusYears(beginContract.getYear()).minusMonths(beginContract.getMonthOfYear()).minusDays(beginContract.getDayOfMonth());
				days = newDate.getDayOfMonth();
				vacationDays = convertWorkDaysToVacationDaysLessThreeYears(days);
				
			}
			else{
				LocalDate beginYear = new LocalDate(now.getYear(),1,1);
				LocalDate newDate = now.minusYears(beginYear.getYear()).minusMonths(beginYear.getMonthOfYear()).minusDays(beginYear.getDayOfMonth());
				days = newDate.getDayOfMonth();
				vacationDays = convertWorkDaysToVacationDaysLessThreeYears(days);
			}
		}
		else{
			LocalDate beginYear = new LocalDate(now.getYear(),1,1); 
			LocalDate newDate = now.minusYears(beginYear.getYear()).minusMonths(beginYear.getMonthOfYear()).minusDays(beginYear.getDayOfMonth());
			days = newDate.getDayOfMonth();
			vacationDays = convertWorkDaysToVacationDaysMoreThreeYears(days);
		}
		
		return vacationDays;
	}
	
	/**
	 * 
	 * @return il numero di giorni di permesso legge maturati nell'anno corrente
	 */
	public int permissionCurrentYear(int year){
		int days = 0;
		int permissionDays = 0;
		LocalDate now = new LocalDate().now();
		now.withYear(year);
		LocalDate beginYear = new LocalDate(now.getYear(),1,1);
		LocalDate newDate = now.minusYears(beginYear.getYear()).minusMonths(beginYear.getMonthOfYear()).minusDays(beginYear.getDayOfMonth());
		days = newDate.getDayOfMonth();
		permissionDays = convertWorkDaysToPermissionDays(days);
		return permissionDays;
	}
	
	/**
	 * 
	 * @return il numero di giorni di ferie avanzati da quelli maturati l'anno precedente e non ancora utilizzati
	 */
	public int vacationLastYearNotYetUsed(int year){
		int actualVacationDays = 0;
		LocalDate date = new LocalDate(year,1,1);
		//recupero dal db le assenze fatte quest'anno che hanno codice 4 ovvero quelle con causale "residuo anno precedente"
		List<Absence> absence = Absence.find("Select abs from Absence abs where abs.person = ? and abs.date > ? " +
				"and abs.absenceType = ?", person, date, 4).fetch();
		LocalDate beginLastYear = new LocalDate(year-1,1,1);
		LocalDate endLastYear = new LocalDate(year-1,12,31);
		//recupero dal db le assenze fatte l'anno precedente che hanno codice 5 ovvero quelle con causale "ferie anno corrente"		
		List<Absence> oldAbsence = Absence.find("Select abs from Absence abs where abs.person = ? and abs.date >= ? and " +
				"abs.date < ? and abs.absenceType = ?", person, beginLastYear, endLastYear, 5).fetch();
		//con questa query vado a prendere il piano ferie previsto per la persona da cui vado a estrarre il numero di giorni di ferie 
		//ad essa attribuiti
		VacationCode vacCode = VacationCode.find("Select vc from VacationCode vc, VacationPeriod vp where vp.vacationCode = vc " +
				"and vp.person = ? order by vp.beginFrom desc", person).first();
		int vacationDays = vacCode.vacationDays;
		int residualVacationDays = vacationDays-oldAbsence.size();
		if(residualVacationDays > 0){
			actualVacationDays = residualVacationDays-absence.size();
			
		}
		
		return actualVacationDays;
	}
	
	/**
	 * 
	 * @return il numero di giorni di ferie residui dall'anno precedente 
	 */
	public int vacationLastYear(int year){
		int days = 0;
		LocalDate beginLastYear = new LocalDate(year-1,1,1);
		LocalDate endLastYear = new LocalDate(year-1,12,31);
		//recupero dal db le assenze fatte l'anno precedente che hanno codice 5 ovvero quelle con causale "ferie anno corrente"		
		List<Absence> oldAbsence = Absence.find("Select abs from Absence abs where abs.person = ? and abs.date >= ? and " +
				"abs.date < ? and abs.absenceType = ?", person, beginLastYear, endLastYear, 5).fetch();
		//con questa query vado a prendere il piano ferie previsto per la persona da cui vado a estrarre il numero di giorni di ferie 
		//ad essa attribuiti
		VacationCode vacCode = VacationCode.find("Select vc from VacationCode vc, VacationPeriod vp where vp.vacationCode = vc " +
				"and vp.person = ? order by vp.beginFrom desc", person).first();
		int vacationDays = vacCode.vacationDays;
		days = vacationDays-oldAbsence.size();
		return days;
	}
	
	/**
	 * 
	 * @return il numero di giorni di permesso che la persona ha da contratto
	 */
	public int personalPermission(){
		int permissionDays = 0;
		VacationCode vacCode = VacationCode.find("Select vc from VacationCode vc, VacationPeriod vp where vp.vacationCode = vc " +
				"and vp.person = ? order by vp.beginFrom desc", person).first();
		permissionDays = vacCode.permissionDays;
		return permissionDays;
	}
	
	/**
	 * 
	 * @param days
	 * @return il numero di giorni di ferie che corrispondono al numero di giorni lavorati dall'inizio dell'anno per chi lavora in 
	 * istituto da meno di tre anni
	 */
	private int convertWorkDaysToVacationDaysLessThreeYears(int days){
		int vacationDays = 0;
		if(days >= 1 && days < 15)
			vacationDays = 0;
		if(days >= 16 && days < 45)
			vacationDays = 2;
		if(days >= 46 && days < 75)
			vacationDays = 4;
		if(days >= 76 && days < 106)
			vacationDays = 6;
		if(days >= 107 && days < 136)
			vacationDays = 8;
		if(days >= 137 && days < 167)
			vacationDays = 10;
		if(days >= 168 && days < 197)
			vacationDays = 13;
		if(days >= 198 && days < 227)
			vacationDays = 15;
		if(days >= 228 && days < 258)
			vacationDays = 17;
		if(days >= 259 && days < 288)
			vacationDays = 19;
		if(days >= 289 && days < 319)
			vacationDays = 21;
		if(days >= 320 && days < 349)
			vacationDays = 23;
		if(days >= 350 && days < 365)
			vacationDays = 26;
		return vacationDays;
	}
	
	/**
	 * 
	 * @param days
	 * @return il numero di giorni di ferie che corrispondono al numero di giorni lavorati dall'inizio dell'anno per chi lavora in
	 * istituto da più di tre anni
	 */
	private int convertWorkDaysToVacationDaysMoreThreeYears(int days){
		int vacationDays = 0;
		if(days >= 1 && days < 15)
			vacationDays = 0;
		if(days >= 16 && days < 45)
			vacationDays = 2;
		if(days >= 46 && days < 75)
			vacationDays = 4;
		if(days >= 76 && days < 106)
			vacationDays = 7;
		if(days >= 107 && days < 136)
			vacationDays = 9;
		if(days >= 137 && days < 167)
			vacationDays = 11;
		if(days >= 168 && days < 197)
			vacationDays = 14;
		if(days >= 198 && days < 227)
			vacationDays = 16;
		if(days >= 228 && days < 258)
			vacationDays = 18;
		if(days >= 259 && days < 288)
			vacationDays = 21;
		if(days >= 289 && days < 319)
			vacationDays = 23;
		if(days >= 320 && days < 349)
			vacationDays = 25;
		if(days >= 350 && days < 365)
			vacationDays = 28;
		return vacationDays;
	}
	
	/**
	 * 
	 * @param days
	 * @return il numero di giorni di permesso legge spettanti al dipendente a seconda dei giorni di presenza
	 */
	private int convertWorkDaysToPermissionDays(int days){
		int permissionDays = 0;
		if(days >= 45 && days < 135)
			permissionDays = 1;
		if(days >= 136 && days < 225)
			permissionDays = 2;
		if(days >= 226 && days < 315)
			permissionDays = 3;
		if(days >= 316 && days < 365)
			permissionDays = 4;
		return permissionDays;
	}
}
