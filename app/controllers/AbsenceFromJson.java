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
 * curl -H "Content-Type: application/json" -X POST -d '{ "dateFrom" : "2014-01-01", "dateTo" : "2014-01-20", "emails" : 
 * [{"email" : "cristian.lucchesi@iit.cnr.it"},{"email" : "stefano.ruberti@iit.cnr.it"}]}' 
 * http://localhost:8888/absenceFromJson/absenceInPeriod
 */
public class AbsenceFromJson extends Controller{

	public static void absenceInPeriod(@As(binder=JsonPersonEmailBinder.class) PersonEmailFromJson body){

		Logger.debug("Received personEmailFromJson %s", body);
		if(body == null)
			badRequest();

		Logger.debug("Entrato nel metodo getAbsenceInPeriod...");
		List<PersonPeriodAbsenceCode> personsToRender = new ArrayList<PersonPeriodAbsenceCode>();
		PersonPeriodAbsenceCode personPeriodAbsenceCode = null;
		LocalDate dateFrom = new LocalDate(body.dateFrom);
		LocalDate dateTo = new LocalDate(body.dateTo);

		for(Person person : body.persons){
			personPeriodAbsenceCode = new PersonPeriodAbsenceCode();

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
					personPeriodAbsenceCode.name = person.name;
					personPeriodAbsenceCode.surname = person.surname;
					personPeriodAbsenceCode.code = previousAbsence.absenceType.code;
					personPeriodAbsenceCode.dateFrom = new String(startCurrentPeriod.getYear()+"-"+startCurrentPeriod.getMonthOfYear()+"-"+startCurrentPeriod.getDayOfMonth());
					personPeriodAbsenceCode.dateTo = new String(endCurrentPeriod.getYear()+"-"+endCurrentPeriod.getMonthOfYear()+"-"+endCurrentPeriod.getDayOfMonth());
					personsToRender.add(personPeriodAbsenceCode);

					previousAbsence = abs;
					startCurrentPeriod = abs.personDay.date;
					endCurrentPeriod = abs.personDay.date;
				}
			}

			if(previousAbsence!=null)
			{
				personPeriodAbsenceCode = new PersonPeriodAbsenceCode();
				personPeriodAbsenceCode.name = person.name;
				personPeriodAbsenceCode.surname = person.surname;
				personPeriodAbsenceCode.code = previousAbsence.absenceType.code;
				personPeriodAbsenceCode.dateFrom = new String(startCurrentPeriod.getYear()+"-"+startCurrentPeriod.getMonthOfYear()+"-"+startCurrentPeriod.getDayOfMonth());
				personPeriodAbsenceCode.dateTo = new String(endCurrentPeriod.getYear()+"-"+endCurrentPeriod.getMonthOfYear()+"-"+endCurrentPeriod.getDayOfMonth());
				personsToRender.add(personPeriodAbsenceCode);
			}
		}

		renderJSON(personsToRender);
	}


	public static void frequentAbsence(@As(binder=JsonRequestedFrequentAbsenceBinder.class) PeriodAbsenceCode body){
		Logger.debug("Received PeriodAbsenceCode %s", body);
		if(body == null)
			badRequest();
		List<FrequentAbsenceCode> frequentAbsenceCodeList = new ArrayList<FrequentAbsenceCode>();
		List<String> listaFerie = new ArrayList<String>();
		List<String> listaMalattie = new ArrayList<String>();
		List<String> listaMissioni = new ArrayList<String>();
		List<String> listaRiposiCompensativi = new ArrayList<String>();
		List<String> listaAltri = new ArrayList<String>();
		List<Absence> absList = Absence.find("Select abs from Absence abs, PersonDay pd " +
				"where abs.personDay = pd and abs.absenceType.absenceTypeGroup is null and pd.date between ? and ?", new LocalDate(body.dateFrom), new LocalDate(body.dateTo)).fetch();
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
			/*
			if((!abs.absenceType.code.equals("92") || abs.absenceType.code.equals("94") || !abs.absenceType.description.contains("Riposo compensativo")
					|| !abs.absenceType.description.contains("malattia") || !abs.absenceType.description.contains("ferie")) && (!listaAltri.contains(abs.absenceType.code))){
			*/
			if(!listaAltri.contains(abs.absenceType.code))
				listaAltri.add(abs.absenceType.code);

			//}
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
			frequentAbsenceCodeFerie.description = "ferie";
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
			frequentAbsenceCodeMalattia.description = "malattia";
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
			frequentAbsenceCodeCompensativi.description = "riposi compensativi";
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

			frequentAbsenceCodeMissione.description = "missione";
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
		if(listaAltri.size() > 0){
			frequentAbsenceCodeAltri.description = "altri";
			if(frequentAbsenceCodeAltri.code.endsWith("-"))
				frequentAbsenceCodeAltri.code = frequentAbsenceCodeAltri.code.substring(0, frequentAbsenceCodeAltri.code.length()-1);

			frequentAbsenceCodeList.add(frequentAbsenceCodeAltri);
		}

		renderJSON(frequentAbsenceCodeList);
	}

}
