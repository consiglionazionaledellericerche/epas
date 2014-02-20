package controllers;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.LocalDate;

import it.cnr.iit.epas.JsonPersonEmailBinder;
import it.cnr.iit.epas.JsonRequestedFrequentAbsenceBinder;
import models.Absence;
import models.AbsenceType;
import models.Person;
import models.exports.FrequentAbsenceCode;
import models.exports.PeriodAbsenceCode;
import models.exports.PersonEmailFromJson;
import models.exports.PersonPeriodAbsenceCode;
import play.Logger;
import play.data.binding.As;
import play.mvc.Controller;



/**
 * 
 * @author dario
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
						endCurrentPeriod = abs.personDay.date;
						continue;
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
	public static void frequentAbsence(Integer yearFrom, Integer monthFrom, Integer dayFrom, Integer yearTo, Integer monthTo, Integer dayTo){
//		Logger.debug("Received PeriodAbsenceCode %s", body);
//		if(body == null)
//			badRequest();
		List<FrequentAbsenceCode> frequentAbsenceCodeList = new ArrayList<FrequentAbsenceCode>();
		List<String> listaFerie = new ArrayList<String>();
		List<String> listaMalattie = new ArrayList<String>();
		List<String> listaMissioni = new ArrayList<String>();
		List<String> listaRiposiCompensativi = new ArrayList<String>();
		List<String> listaAltri = new ArrayList<String>();
		LocalDate dateFrom = new LocalDate(params.get("yearFrom", Integer.class), params.get("monthFrom", Integer.class), params.get("dayFrom", Integer.class));
		LocalDate dateTo = new LocalDate(params.get("yearTo", Integer.class), params.get("monthTo", Integer.class), params.get("dayTo", Integer.class));
		List<Absence> absList = Absence.find("Select abs from Absence abs, PersonDay pd " +
				"where abs.personDay = pd and abs.absenceType.absenceTypeGroup is null and pd.date between ? and ?", dateFrom, dateTo).fetch();
		for(Absence abs : absList){
			
			Logger.debug("Trovato codice di assenza %s in data %s", abs.absenceType.code, abs.personDay.date);
			
			if((abs.absenceType.description.contains("ferie") || abs.absenceType.code.equals("94")) )
			{
				if(!listaFerie.contains(abs.absenceType.code))
					listaFerie.add(abs.absenceType.code);
				continue;
			}
			
			if(abs.absenceType.description.contains("malattia") )
			{ 
				if(!listaMalattie.contains(abs.absenceType.code))
					listaMalattie.add(abs.absenceType.code);
				continue;
			}
			
			if(abs.absenceType.description.contains("Riposo compensativo") )
			{ 
				if(!listaRiposiCompensativi.contains(abs.absenceType.code))
					listaRiposiCompensativi.add(abs.absenceType.code);
				continue;
			}
			
			if(abs.absenceType.code.equals("92")) 
			{
				if(!listaMissioni.contains(abs.absenceType.code))
					listaMissioni.add(abs.absenceType.code);
				continue;
			}
			
			if(!listaAltri.contains(abs.absenceType.code))
				listaAltri.add(abs.absenceType.code);
		}		

		Logger.debug("Liste di codici di assenza completate con dimensioni: %d %d %d %d %d", 
				listaFerie.size(), listaMalattie.size(), listaMissioni.size(), listaRiposiCompensativi.size(), listaAltri.size());
		
		FrequentAbsenceCode frequentAbsenceCodeFerie = new FrequentAbsenceCode("","");
		int ferieSize = listaFerie.size();
		for(String abs : listaFerie){
			if(ferieSize > 0)
				frequentAbsenceCodeFerie.code = frequentAbsenceCodeFerie.code+abs+"-";
			else
				frequentAbsenceCodeFerie.code = frequentAbsenceCodeFerie.code+abs;

			ferieSize--;
		}
		if(listaFerie.size() > 0){
			frequentAbsenceCodeFerie.description = "Ferie";
			if(frequentAbsenceCodeFerie.code.endsWith("-"))
				frequentAbsenceCodeFerie.code = frequentAbsenceCodeFerie.code.substring(0, frequentAbsenceCodeFerie.code.length()-1);
			frequentAbsenceCodeList.add(frequentAbsenceCodeFerie);
		}

		FrequentAbsenceCode frequentAbsenceCodeMalattia = new FrequentAbsenceCode("","");
		int malattiaSize = listaMalattie.size();
		for(String abs : listaMalattie){
			if(malattiaSize > 0)
				frequentAbsenceCodeMalattia.code = frequentAbsenceCodeMalattia.code+abs+"-";
			else
				frequentAbsenceCodeMalattia.code = frequentAbsenceCodeMalattia.code+abs;

			malattiaSize--;
		}
		if(listaMalattie.size() > 0){
			frequentAbsenceCodeMalattia.description = "Malattia";
			if(frequentAbsenceCodeMalattia.code.endsWith("-"))
				frequentAbsenceCodeMalattia.code = frequentAbsenceCodeMalattia.code.substring(0, frequentAbsenceCodeMalattia.code.length()-1);
			frequentAbsenceCodeList.add(frequentAbsenceCodeMalattia);
		}

		FrequentAbsenceCode frequentAbsenceCodeCompensativi = new FrequentAbsenceCode("","");
		int compensativiSize = listaRiposiCompensativi.size();
		for(String abs : listaRiposiCompensativi){
			if(compensativiSize > 0)
				frequentAbsenceCodeCompensativi.code = frequentAbsenceCodeCompensativi.code+abs+"-";
			else
				frequentAbsenceCodeCompensativi.code = frequentAbsenceCodeCompensativi.code+abs;

			compensativiSize--;
		}
		if(listaRiposiCompensativi.size() > 0){
			frequentAbsenceCodeCompensativi.description = "Riposo compensativo";
			if(frequentAbsenceCodeCompensativi.code.endsWith("-"))
				frequentAbsenceCodeCompensativi.code = frequentAbsenceCodeCompensativi.code.substring(0, frequentAbsenceCodeCompensativi.code.length()-1);
			
			frequentAbsenceCodeList.add(frequentAbsenceCodeCompensativi);
		}

		FrequentAbsenceCode frequentAbsenceCodeMissione = new FrequentAbsenceCode("","");
		int missioneSize = listaMissioni.size();
		for(String abs : listaMissioni){
			if(missioneSize > 0)
				frequentAbsenceCodeMissione.code = frequentAbsenceCodeMissione.code+abs+"-";
			else
				frequentAbsenceCodeMissione.code = frequentAbsenceCodeMissione.code+abs;

			missioneSize--;
		}
		if(listaMissioni.size() > 0){
			if(frequentAbsenceCodeMissione.code.endsWith("-"))
				frequentAbsenceCodeMissione.code = frequentAbsenceCodeMissione.code.substring(0, frequentAbsenceCodeMissione.code.length()-1);

			frequentAbsenceCodeMissione.description = "Missione";
			frequentAbsenceCodeList.add(frequentAbsenceCodeMissione);
		}

		FrequentAbsenceCode frequentAbsenceCodeAltri = new FrequentAbsenceCode("","");
		int altriSize = listaAltri.size();
		for(String abs : listaAltri){
			if(altriSize > 0)
				frequentAbsenceCodeAltri.code = frequentAbsenceCodeAltri.code+abs+"-";
			else
				frequentAbsenceCodeAltri.code = frequentAbsenceCodeAltri.code+abs;

			altriSize--;
		}
		
		/* Arianna */
		/*if(listaAltri.size() > 0){
			frequentAbsenceCodeAltri.description = "Assenza generica";
			if(frequentAbsenceCodeAltri.code.endsWith("-"))
				frequentAbsenceCodeAltri.code = frequentAbsenceCodeAltri.code.substring(0, frequentAbsenceCodeAltri.code.length()-1);

			frequentAbsenceCodeList.add(frequentAbsenceCodeAltri);
		}*/

		renderJSON(frequentAbsenceCodeList);
	}

}
