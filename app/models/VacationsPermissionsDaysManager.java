package models;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import models.enumerate.AccumulationBehaviour;

import org.hibernate.ejb.AvailableSettings;
import org.joda.time.LocalDate;

import play.db.jpa.JPA;

public class VacationsPermissionsDaysManager {
	
	public final static class AvailabilityInfo{
		public boolean isAvailable = false;
		public boolean isToBeReplaced = false;
		public AbsenceType abt;
		public String reason;
		
		public AvailabilityInfo(boolean isAvailable, boolean isToBeReplaced, AbsenceType abt){
			this.abt = abt;
			this.isAvailable = isAvailable;
			this.isToBeReplaced = isToBeReplaced;
		}
		public AvailabilityInfo(String reason){
			this.reason = reason;
		}
		public AvailabilityInfo(boolean isAvailable){
			this.isAvailable = isAvailable;
			
		}
	}
	
	public static AvailabilityInfo isAvailable(AbsenceType abt, Person person, LocalDate date){
		if(person == null || abt == null || date == null)
			throw new IllegalArgumentException("Person and absenceType and date must be not null...");
		
		if(abt.validTo.isBefore(date) || abt.validFrom.isAfter(date))
			return new AvailabilityInfo(String.format("La data di validità del codice %s " +
					"non è compatibile con la data %s", abt.code, date));
		if(!abt.qualifications.contains(person.qualification))
			return new AvailabilityInfo(String.format("La qualifica della persona %s " +
					"non è compatibile con il codice %s che si desidera inserire", person, abt.code));
		
		/**
		 * si controlla a quale tipologia di assenza appartiene il codice di assenza passato: ferie (32), ferie anno precedente (31)
		 * permesso legge (94), recupero (91)
		 */
		if(abt.code.equals("32")){
			return isVacationCurrentYearAvailable(person, date);
		}
		if(abt.code.equals("31")){
			return isVacationPastYearAvailable(person,date);
		}
		if(abt.code.equals("94")){
			return isPermissionDaysAvailable(person, date);
		}
		if(abt.code.equals("91")){
			return isRecoveryDaysAvailable(person, date);
		}	
		
		/**
		 * le limitazioni sono sulle assenze di tipo orario che appartengono a gruppi o alle assenze precedentemente controllate
		 */
		if(abt.absenceTypeGroup == null)
			return new AvailabilityInfo(true);
		
		return isAbsenceTypeWithGroupAvailable(abt, person, date);
		
	}

	private static AvailabilityInfo isRecoveryDaysAvailable(Person person,
			LocalDate date) {
		/**
		 * bisogna controllare a quale livello appartiene la persona per poter decidere fino a che punto può usufruire dei giorni di recupero
		 * (vedere nella configurazione) e quanti al massimo può averne durante l'anno.
		 */
		Configuration config = Configuration.find("Select conf from Configuration conf where conf.beginDate < ? and conf.endDate > ?", 
				date, date).first();
		PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, date).first();
		/**
		 * controllo che il mese in cui risiede la data sia gennaio di modo da passare come residuo, quello alla fine di dicembre dell'anno 
		 * precedente
		 */
		int month = 0;
		int year = 0;
		if(date.getMonthOfYear() == 1){
			month = 12;
			year = date.getYear()-1;
		}
		else{
			month = date.getMonthOfYear();
			year = date.getYear();
		}
			
		PersonMonth pm = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and pm.month = ? and pm.year = ?", 
				person, month, year).first();
		if(pd == null || pm == null)
			throw new IllegalStateException(String.format("Person day o person month non valido per %s %s", person.name, person.surname));
		if(pd.progressive + pm.remainingHours > config.minimumRemainingTimeToHaveRecoveryDay ){
			if(person.qualification.qualification == 1 || person.qualification.qualification == 2 || person.qualification.qualification == 3){
				int expireMonth = config.monthExpireRecoveryDaysOneThree;
				if(expireMonth > 12 || expireMonth > date.getMonthOfYear())
					return new AvailabilityInfo(true);
				else{
					return new AvailabilityInfo("Il giorno di recupero non può essere preso poichè la data è superiore alla scadenza" +
							"entro la quale i giorni di recupero possono essere presi.");
				}
					
			}
			else{
				int expireMonth = config.monthExpireRecoveryDaysFourNine;
				if(expireMonth > date.getMonthOfYear()){
					return new AvailabilityInfo(true);
				}
				else{
					return new AvailabilityInfo("Il giorno di recupero non può essere preso poichè la data è superiore alla scadenza" +
							"entro la quale i giorni di recupero possono essere presi.");
				}
			}
		}
		return null;
	}

	private static AvailabilityInfo isPermissionDaysAvailable(Person person,
			LocalDate date) {
		AvailabilityInfo permissionDaysAvailable = isPermissionCurrentYearAvailable();
		return permissionDaysAvailable;
	}

	private static AvailabilityInfo isVacationPastYearAvailable(Person person,
			LocalDate date) {
		/**
		 * per prima cosa si controlla che sia possibile usare ferie dell'anno precedente, ovvero si guarda se non è scaduto il periodo 
		 * utile per poterle utilizare
		 */
		Configuration config = Configuration.find("Select conf from Configuration conf where conf.beginDate < ? and conf.endDate > ?", 
				date, date).first();
		if(date.isAfter(new LocalDate(date.getYear(),config.monthExpiryVacationPastYear,config.dayExpiryVacationPastYear))){
			return new AvailabilityInfo(false);
		}
		YearRecap yr = new YearRecap();
		int vacationDaysPastYearAvailable = yr.vacationLastYearNotYetUsed();
		if(vacationDaysPastYearAvailable>0)
			return new AvailabilityInfo(true);
		return new AvailabilityInfo(false);
	}

	private static AvailabilityInfo isVacationCurrentYearAvailable(Person person,
			LocalDate date) {
		AvailabilityInfo availableVacationPastYear = isVacationPastYearAvailable(person, date);
		if(availableVacationPastYear.isAvailable){
			AbsenceType abt = AbsenceType.find("Select abt from AbsenceType abt where abt.code = ?", "31").first();
			return new AvailabilityInfo(true, true, abt);
		}
		else{
			AbsenceType abt = AbsenceType.find("Select abt from AbsenceType abt where abt.code = ?", "32").first();
			return new AvailabilityInfo(true, true, abt);
		}			
		
	}
	
	private static AvailabilityInfo isAbsenceTypeWithGroupAvailable(AbsenceType abt, Person person, LocalDate date){
		/**
		 * se il comportamento in caso di raggiungimento del limite è "non fare niente", è inutile fare ulteriori controlli
		 */
		
		if(abt.absenceTypeGroup.accumulationBehaviour.equals(AccumulationBehaviour.nothing))
			return new AvailabilityInfo(true);
		
		AbsenceTypeGroup abtg = abt.absenceTypeGroup;
		
		String initQuery = "Select sum(abt.justifiedWorkTime) from AbsenceType abt join Absence abs " +
				"where abt.absenceTypeGroup = :abtg";
		
		Query query =  JPA.em().createQuery(initQuery + " and abs.date between :begin and :end " +
				"group by abt.absenceTypeGroup")
				.setParameter("abtg", abtg);
		
		
		switch(abtg.accumulationType){
			case yearly:
				query
					.setParameter("begin", date.withDayOfYear(1))
					.setParameter("end", date);
				break;
				
			case monthly:
				query
					.setParameter("begin", date.withDayOfMonth(1))
					.setParameter("end", date);
				break;
					
			case always:
				query = JPA.em().createQuery(initQuery)
					.setParameter("abtg", abtg);
				break;
			default:
				throw new IllegalStateException(String.format("AccumulationType %s non riconosciuto", abtg.accumulationType));
				
		}
		Integer currentTotalTime = (Integer)query.getSingleResult();
		switch(abtg.accumulationBehaviour){
			case noMoreAbsencesAccepted:
				if(currentTotalTime + abt.justifiedWorkTime > abtg.limitInMinute)
					return new AvailabilityInfo(String.format("Nel periodo '%s' sono già stati presi %s minuti per il gruppo di assenze" +
							" %s. Il limite per questo gruppo di assenze è %s. Quindi non è possibile prendere ulteriori %s minuti", 
							abtg.accumulationType, currentTotalTime, abtg.label, abtg.limitInMinute, abt.justifiedWorkTime));
				else
					return new AvailabilityInfo(true);
				/**
				 * nel caso in cui debba rimpiazzare il codice orario con il suo codice di completamento giornaliero, controllo che quel codice
				 * giornaliero non abbia raggiunto il suo limite di utilizzo
				 */
			case replaceCodeAndDecreaseAccumulation:
				AvailabilityInfo availabilityReplaceCode = isAvailable(abt.absenceTypeGroup.replacingAbsenceType, person, date);
				if(availabilityReplaceCode.isAvailable)
					return new AvailabilityInfo(true);
				return new AvailabilityInfo(String.format("Il codice %s dovrebbe essere affiancato da %s perchè sono stati già presi" +
						"%s minuti per il gruppo di assenze %s. Questo però non è possibile poichè: %s",
						abt.code, abtg.replacingAbsenceType.code, currentTotalTime, abtg.label, availabilityReplaceCode.reason));
			default:
				throw new IllegalStateException(String.format("AccumulationType %s non riconosciuto", abtg.accumulationType));
			
		}

	}

	
	public static AvailabilityInfo isPermissionCurrentYearAvailable(){
		YearRecap yr = new YearRecap();
		int permissionDaysAvailable = yr.permissionCurrentYear() - yr.personalPermissionUsed();
		if(permissionDaysAvailable > 0)
			return new AvailabilityInfo(true);
		
		return new AvailabilityInfo(false);
	}
	
	public static void main(String[]args){
		LocalDate data = new LocalDate();
		System.out.print(data.withDayOfYear(1));
		
	}
}
