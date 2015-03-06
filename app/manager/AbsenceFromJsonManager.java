package manager;

import helpers.ModelQuery;

import java.util.ArrayList;
import java.util.List;

import models.Absence;
import models.Person;
import models.exports.FrequentAbsenceCode;
import models.exports.PersonEmailFromJson;
import models.exports.PersonPeriodAbsenceCode;
import models.query.QAbsence;
import models.query.QPersonDay;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;

import dao.AbsenceDao;

public class AbsenceFromJsonManager {
	
	private final static Logger log = LoggerFactory.getLogger(AbsenceFromJsonManager.class);

	/**
	 * 
	 * @param body
	 * @param dateFrom
	 * @param dateTo
	 * @return la lista dei PersonPeriodAbsenceCode nel periodo compreso tra 'dateFrom' e 'dateTo' per le persone recuperate dal
	 * 'body' contenente la lista delle persone ricavate dalle email arrivate via chiamata post json
	 */
	public static List<PersonPeriodAbsenceCode> getPersonForAbsenceFromJson(PersonEmailFromJson body, LocalDate dateFrom, LocalDate dateTo){
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

				List<Absence> absences = AbsenceDao.getAbsencesInPeriod(Optional.fromNullable(person), dateFrom, Optional.fromNullable(dateTo), false);

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
	
	
	/**
	 * 
	 * @param dateFrom
	 * @param dateTo
	 * @return la lista dei frequentAbsenceCode, ovvero dei codici di assenza più frequentemente usati nel periodo compreso tra
	 * 'dateFrom' e 'dateTo'
	 */
	public static List<FrequentAbsenceCode> getFrequentAbsenceCodeForAbsenceFromJson(LocalDate dateFrom, LocalDate dateTo){
		List<FrequentAbsenceCode> frequentAbsenceCodeList = new ArrayList<FrequentAbsenceCode>();
		QAbsence absence = QAbsence.absence;
		QPersonDay personDay = QPersonDay.personDay;
		
		BooleanBuilder conditions = new BooleanBuilder(personDay.date.between(dateFrom, dateTo));
		 
		JPQLQuery queryRiposo = ModelQuery.queryFactory().from(absence).join(absence.personDay, personDay)
				.where(
						new BooleanBuilder(conditions)
						.and(absence.absenceType.description.containsIgnoreCase("Riposo compensativo"))
					);
		List<String> listaRiposiCompensativi = queryRiposo.distinct().list(absence.absenceType.code);
		
		JPQLQuery queryferieOr94 = ModelQuery.queryFactory().from(absence).join(absence.personDay, personDay)
				.where(
						new BooleanBuilder(conditions)
						.and(
								absence.absenceType.description.containsIgnoreCase("ferie")
								.or(absence.absenceType.code.eq("94"))
							)
					);
		List<String> listaFerie = queryferieOr94.distinct().list(absence.absenceType.code);
		 
		JPQLQuery queryMissione = ModelQuery.queryFactory().from(absence).join(absence.personDay, personDay)
				.where(
						new BooleanBuilder(conditions)
						.and(absence.absenceType.code.eq("92"))
						);
		List<String> listaMissioni = queryMissione.distinct().list(absence.absenceType.code);

		log.debug("Liste di codici di assenza completate con dimensioni: {} {} {}", 
				new Object[] {listaFerie.size(), listaMissioni.size(), listaRiposiCompensativi.size()});

		Joiner joiner = Joiner.on("-").skipNulls();
		
		frequentAbsenceCodeList.add(new FrequentAbsenceCode(joiner.join(listaFerie),"Ferie"));
		frequentAbsenceCodeList.add(new FrequentAbsenceCode(joiner.join(listaRiposiCompensativi),"Riposo compensativo"));
		frequentAbsenceCodeList.add(new FrequentAbsenceCode(joiner.join(listaMissioni),"Missione"));		
		
		log.info("Lista di codici trovati: {}", frequentAbsenceCodeList);
		return frequentAbsenceCodeList;
	}
}
