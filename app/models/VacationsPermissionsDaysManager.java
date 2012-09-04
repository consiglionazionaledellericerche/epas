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
		
		if(abt.validTo.compareTo(date.toDate()) < 0 || abt.validFrom.compareTo(date.toDate()) > 0)
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
		// TODO Auto-generated method stub
		return null;
	}

	private static AvailabilityInfo isPermissionDaysAvailable(Person person,
			LocalDate date) {
		// TODO Auto-generated method stub
		return null;
	}

	private static AvailabilityInfo isVacationPastYearAvailable(Person person,
			LocalDate date) {
		
		return null;
	}

	private static AvailabilityInfo isVacationCurrentYearAvailable(Person person,
			LocalDate date) {
		AvailabilityInfo availableVacationPastYear = isVacationPastYearAvailable(person, date);
		if(availableVacationPastYear.isAvailable){
			AbsenceType abt = AbsenceType.find("Select abt from AbsenceType abt where abt.code = ?", "31").first();
			return new AvailabilityInfo(true, true, abt);
		}
		
			
		return null;
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
	
	public static void main(String[]args){
		LocalDate data = new LocalDate();
		System.out.print(data.withDayOfYear(1));
		
	}
}
