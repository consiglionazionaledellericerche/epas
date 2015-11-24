package manager;

import com.google.common.base.Optional;
import dao.AbsenceDao;
import models.Absence;
import models.Person;
import models.exports.PersonEmailFromJson;
import models.exports.PersonPeriodAbsenceCode;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class AbsenceFromJsonManager {

	private final static Logger log = LoggerFactory.getLogger(AbsenceFromJsonManager.class);
	@Inject
	private AbsenceDao absenceDao;

	/**
	 * 
	 * @param body
	 * @param dateFrom
	 * @param dateTo
	 * @return la lista dei PersonPeriodAbsenceCode nel periodo compreso tra 'dateFrom' e 'dateTo' per le persone recuperate dal
	 * 'body' contenente la lista delle persone ricavate dalle email arrivate via chiamata post json
	 */
	public List<PersonPeriodAbsenceCode> getPersonForAbsenceFromJson(PersonEmailFromJson body, LocalDate dateFrom, LocalDate dateTo){
		List<PersonPeriodAbsenceCode> personsToRender = new ArrayList<PersonPeriodAbsenceCode>();
		PersonPeriodAbsenceCode personPeriodAbsenceCode = null;

		String meseInizio = "";
		String meseFine = "";
		String giornoInizio = "";
		String giornoFine = "";
		for(Person person : body.persons){
			personPeriodAbsenceCode = new PersonPeriodAbsenceCode();
			if(person != null){
				log.debug("Controllo {}", person.getFullname());

				List<Absence> absences = absenceDao.getAbsencesInPeriod(Optional.fromNullable(person), dateFrom, Optional.fromNullable(dateTo), false);

				log.debug("Lista assenze per {}: {}", person.getFullname(), absences);

				LocalDate startCurrentPeriod = null;
				LocalDate endCurrentPeriod = null;
				Absence previousAbsence = null;
				for(Absence abs : absences){

					if(previousAbsence == null){
						previousAbsence = abs;
						startCurrentPeriod = abs.personDay.date;
						endCurrentPeriod = abs.personDay.date;
						continue;
					}
					if(abs.absenceType.code.equals(previousAbsence.absenceType.code)){
						if(!endCurrentPeriod.isEqual(abs.personDay.date.minusDays(1))){
							personPeriodAbsenceCode = new PersonPeriodAbsenceCode();
							personPeriodAbsenceCode.personId = person.id;
							personPeriodAbsenceCode.name = person.name;
							personPeriodAbsenceCode.surname = person.surname;
							personPeriodAbsenceCode.code = previousAbsence.absenceType.code;
							if(startCurrentPeriod.getMonthOfYear() < 10)
								meseInizio = new String("0"+startCurrentPeriod.getMonthOfYear());
							else
								meseInizio = new String(""+startCurrentPeriod.getMonthOfYear());
							if(endCurrentPeriod.getMonthOfYear() < 10)
								meseFine = new String("0"+endCurrentPeriod.getMonthOfYear());
							else
								meseFine = new String(""+endCurrentPeriod.getMonthOfYear());

							if(startCurrentPeriod.getDayOfMonth() < 10)
								giornoInizio = new String("0"+startCurrentPeriod.getDayOfMonth());
							else
								giornoInizio = new String(""+startCurrentPeriod.getDayOfMonth());
							if(endCurrentPeriod.getDayOfMonth() < 10)
								giornoFine = new String("0"+endCurrentPeriod.getDayOfMonth());
							else
								giornoFine = new String(""+endCurrentPeriod.getDayOfMonth());
							personPeriodAbsenceCode.start = new String(startCurrentPeriod.getYear()+"-"+meseInizio+"-"+giornoInizio);
							personPeriodAbsenceCode.end = new String(endCurrentPeriod.getYear()+"-"+meseFine+"-"+giornoFine);
							personsToRender.add(personPeriodAbsenceCode);

							previousAbsence = abs;
							startCurrentPeriod = abs.personDay.date;
							endCurrentPeriod = abs.personDay.date;
							continue;

						}
						else{
							endCurrentPeriod = abs.personDay.date;
							continue;
						}
					}
					else
					{
						personPeriodAbsenceCode = new PersonPeriodAbsenceCode();
						personPeriodAbsenceCode.personId = person.id;
						personPeriodAbsenceCode.name = person.name;
						personPeriodAbsenceCode.surname = person.surname;
						personPeriodAbsenceCode.code = previousAbsence.absenceType.code;

						if(startCurrentPeriod.getMonthOfYear() < 10)
							meseInizio = new String("0"+startCurrentPeriod.getMonthOfYear());
						else
							meseInizio = new String(""+startCurrentPeriod.getMonthOfYear());
						if(endCurrentPeriod.getMonthOfYear() < 10)
							meseFine = new String("0"+endCurrentPeriod.getMonthOfYear());
						else
							meseFine = new String(""+endCurrentPeriod.getMonthOfYear());

						if(startCurrentPeriod.getDayOfMonth() < 10)
							giornoInizio = new String("0"+startCurrentPeriod.getDayOfMonth());
						else
							giornoInizio = new String(""+startCurrentPeriod.getDayOfMonth());
						if(endCurrentPeriod.getDayOfMonth() < 10)
							giornoFine = new String("0"+endCurrentPeriod.getDayOfMonth());
						else
							giornoFine = new String(""+endCurrentPeriod.getDayOfMonth());
						personPeriodAbsenceCode.start = new String(startCurrentPeriod.getYear()+"-"+meseInizio+"-"+giornoInizio);
						personPeriodAbsenceCode.end = new String(endCurrentPeriod.getYear()+"-"+meseFine+"-"+giornoFine);
						personsToRender.add(personPeriodAbsenceCode);

						previousAbsence = abs;
						startCurrentPeriod = abs.personDay.date;
						endCurrentPeriod = abs.personDay.date;
					}
				}

				if(previousAbsence!=null)
				{
					personPeriodAbsenceCode = new PersonPeriodAbsenceCode();
					personPeriodAbsenceCode.personId = person.id;
					personPeriodAbsenceCode.name = person.name;
					personPeriodAbsenceCode.surname = person.surname;
					personPeriodAbsenceCode.code = previousAbsence.absenceType.code;
					if(startCurrentPeriod.getMonthOfYear() < 10)
						meseInizio = new String("0"+startCurrentPeriod.getMonthOfYear());
					else
						meseInizio = new String(""+startCurrentPeriod.getMonthOfYear());
					if(endCurrentPeriod.getMonthOfYear() < 10)
						meseFine = new String("0"+endCurrentPeriod.getMonthOfYear());
					else
						meseFine = new String(""+endCurrentPeriod.getMonthOfYear());

					if(startCurrentPeriod.getDayOfMonth() < 10)
						giornoInizio = new String("0"+startCurrentPeriod.getDayOfMonth());
					else
						giornoInizio = new String(""+startCurrentPeriod.getDayOfMonth());
					if(endCurrentPeriod.getDayOfMonth() < 10)
						giornoFine = new String("0"+endCurrentPeriod.getDayOfMonth());
					else
						giornoFine = new String(""+endCurrentPeriod.getDayOfMonth());
					personPeriodAbsenceCode.start = new String(startCurrentPeriod.getYear()+"-"+meseInizio+"-"+giornoInizio);
					personPeriodAbsenceCode.end = new String(endCurrentPeriod.getYear()+"-"+meseFine+"-"+giornoFine);
					personsToRender.add(personPeriodAbsenceCode);
				}
			}
			else{
				log.error("Richiesta persona non presente in anagrafica. Possibile sia un non strutturato.");
			}
		}
		return personsToRender;
	}
}
