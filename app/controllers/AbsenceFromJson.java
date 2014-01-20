package controllers;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.LocalDate;

import it.cnr.iit.epas.JsonPersonEmailBinder;
import models.Absence;
import models.AbsenceType;
import models.Person;
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
			AbsenceType abt = null;

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

}
