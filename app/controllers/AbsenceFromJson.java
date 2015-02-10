package controllers;

import helpers.ModelQuery;
import it.cnr.iit.epas.JsonPersonEmailBinder;

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

import com.google.common.base.Joiner;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;

import play.Logger;
import play.data.binding.As;
import play.mvc.Controller;



/**
 * 
 * @author dario
 * @author arianna
 * curl -H "Content-Type: application/json" -X POST -d '{"emails" : 
 * [{"email" : "cristian.lucchesi@iit.cnr.it"},{"email" : "stefano.ruberti@iit.cnr.it"}]}' 
 * http://localhost:8888/absenceFromJson/absenceInPeriod
 */
public class AbsenceFromJson extends Controller{

	public static void absenceInPeriod(Integer yearFrom, Integer monthFrom, Integer dayFrom, Integer yearTo, Integer monthTo, Integer dayTo,
			@As(binder=JsonPersonEmailBinder.class) PersonEmailFromJson body){

		Logger.debug("Received personEmailFromJson %s", body);
		if(body == null)
			badRequest();

		Logger.debug("Entrato nel metodo getAbsenceInPeriod...");
		List<PersonPeriodAbsenceCode> personsToRender = new ArrayList<PersonPeriodAbsenceCode>();
		PersonPeriodAbsenceCode personPeriodAbsenceCode = null;
		LocalDate dateFrom = null;
		LocalDate dateTo  = null;
		if(yearFrom != null && monthFrom != null && dayFrom != null)
			dateFrom = new LocalDate(yearFrom, monthFrom, dayFrom);
		else 
			dateFrom = new LocalDate(params.get("yearFrom", Integer.class), params.get("monthFrom", Integer.class), params.get("dayFrom", Integer.class));
		
		if(yearTo != null && monthTo != null && dayTo != null)
			dateTo = new LocalDate(yearTo, monthTo, dayTo);
		else
			dateTo = new LocalDate(params.get("yearTo", Integer.class), params.get("monthTo", Integer.class), params.get("dayTo", Integer.class));

		String meseInizio = "";
		String meseFine = "";
		String giornoInizio = "";
		String giornoFine = "";
		for(Person person : body.persons){
			personPeriodAbsenceCode = new PersonPeriodAbsenceCode();
			if(person != null){
				Logger.debug("Controllo %s %s", person.name, person.surname);

				List<Absence> absences = Absence.find("Select abs from Absence abs, PersonDay pd " +
						"where abs.personDay = pd and pd.date between ? and ? and pd.person = ? order by abs.personDay.date", 
						dateFrom, dateTo, person).fetch();
				Logger.debug("Lista assenze per %s %s: %s", person.name, person.surname, absences.toString());

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
				Logger.error("Richiesta persona non presente in anagrafica. Possibile sia un non strutturato.");

			}

		}

		renderJSON(personsToRender);
	}
	
	/**
	 * metodo esposto per ritornare la lista dei codici di assenza presi 
	 */
	public static void frequentAbsence(Integer yearFrom, Integer monthFrom, Integer dayFrom, Integer yearTo, Integer monthTo, Integer dayTo) {
		//		Logger.debug("Received PeriodAbsenceCode %s", body);
		//		if(body == null)
		//			badRequest();
		List<FrequentAbsenceCode> frequentAbsenceCodeList = new ArrayList<FrequentAbsenceCode>();
		
		LocalDate dateFrom = new LocalDate(params.get("yearFrom", Integer.class), params.get("monthFrom", Integer.class), params.get("dayFrom", Integer.class));
		LocalDate dateTo = new LocalDate(params.get("yearTo", Integer.class), params.get("monthTo", Integer.class), params.get("dayTo", Integer.class));
		
		QAbsence absence = QAbsence.absence;
		QPersonDay personDay = QPersonDay.personDay;
		
		BooleanBuilder conditions = new BooleanBuilder(personDay.date.between(dateFrom, dateTo));
		 
		JPQLQuery queryRiposo = ModelQuery.queryFactory().from(absence).join(absence.personDay, personDay)
				.where(
						new BooleanBuilder(conditions)
						.and(absence.absenceType.description.containsIgnoreCase("Riposo compensativo"))
					);
		List<String> listaRiposiCompensativi = queryRiposo.distinct().list(absence.absenceType.code);
		//Logger.debug("listaRipoCompansitivi = %s, conditions = %s", listaRiposiCompensativi, new BooleanBuilder(conditions).and(absence.absenceType.description.containsIgnoreCase("Riposo compensativo")));

		JPQLQuery queryferieOr94 = ModelQuery.queryFactory().from(absence).join(absence.personDay, personDay)
				.where(
						new BooleanBuilder(conditions)
						.and(
								absence.absenceType.description.containsIgnoreCase("ferie")
								.or(absence.absenceType.code.eq("94"))
							)
					);
		List<String> listaFerie = queryferieOr94.distinct().list(absence.absenceType.code);
		 
//		JPQLQuery queryMalattia = ModelQuery.queryFactory().from(absence).join(absence.personDay, personDay).where(new BooleanBuilder(conditions).and(absence.absenceType.description.containsIgnoreCase("malattia")));
//		List<String> listaMalattie = queryMalattia.distinct().list(absence.absenceType.code);
		
		JPQLQuery queryMissione = ModelQuery.queryFactory().from(absence).join(absence.personDay, personDay)
				.where(
						new BooleanBuilder(conditions)
						.and(absence.absenceType.code.eq("92"))
						);
		List<String> listaMissioni = queryMissione.distinct().list(absence.absenceType.code);

		Logger.debug("Liste di codici di assenza completate con dimensioni: %d %d %d", 
				listaFerie.size(), listaMissioni.size(), listaRiposiCompensativi.size());

		Joiner joiner = Joiner.on("-").skipNulls();
		
		frequentAbsenceCodeList.add(new FrequentAbsenceCode(joiner.join(listaFerie),"Ferie"));
		frequentAbsenceCodeList.add(new FrequentAbsenceCode(joiner.join(listaRiposiCompensativi),"Riposo compensativo"));
		frequentAbsenceCodeList.add(new FrequentAbsenceCode(joiner.join(listaMissioni),"Missione"));		
		//frequentAbsenceCodeList.add(new FrequentAbsenceCode(joiner.join(listaMalattie),"Malattia"));

		Logger.info("Lista di codici trovati: %s", frequentAbsenceCodeList);
		
		renderJSON(frequentAbsenceCodeList);
	}

}
