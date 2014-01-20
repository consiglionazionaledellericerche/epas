package controllers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.joda.time.LocalDate;

import it.cnr.iit.epas.JsonPersonEmailBinder;
import models.Absence;
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
		PersonPeriodAbsenceCode personPeriodAbsenceCode = new PersonPeriodAbsenceCode();
		for(Person person : body.persons){
			Logger.debug("Le date sono: %s %s", body.dateFrom, body.dateTo);
			personPeriodAbsenceCode.name = person.name;
			personPeriodAbsenceCode.surname = person.surname;
			personPeriodAbsenceCode.dateFrom = body.dateFrom != null || !body.dateFrom.equals("") ? body.dateFrom : new String("");
			personPeriodAbsenceCode.dateTo = body.dateTo != null || !body.dateFrom.equals("") ? body.dateFrom : new String("");
			Absence absence = Absence.find("Select abs from Absence abs, PersonDay pd " +
					"where abs.personDay = pd and pd.date between ? and ? and pd.person = ?", 
					new LocalDate(body.dateFrom), new LocalDate(body.dateTo), person).first();
			
			personPeriodAbsenceCode.code = absence.absenceType.code;
			personsToRender.add(personPeriodAbsenceCode);
			Logger.debug("asdsadsdasdsd");
		}
		
		renderJSON(personPeriodAbsenceCode);
	}

}
