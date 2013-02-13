package controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.LocalDate;

import models.Absence;
import models.Competence;
import models.Person;
import models.PersonDay;
import play.mvc.Controller;
import play.mvc.With;
import play.Logger;

@With( {Secure.class, NavigationMenu.class} )
public class PersonMonths extends Controller{
	
	@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void hourRecap(Long personId,int year){
		Person person = null;
		if(personId != null)
			person = Person.findById(personId);
		else
			person = Security.getPerson();
		Map<Integer, List<Integer>> mapMonthSituation = new HashMap<Integer, List<Integer>>();
		List<Integer> lista = null;
		Integer compensatoryRest = 0;
		for(int month = 1; month < 13; month++){
			LocalDate date = new LocalDate(year, month, 1);
			PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", 
					person, date.dayOfMonth().withMaximumValue()).first();
			Logger.debug("Il person day scansionato è: %s", pd);
			List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?", 
					person, date, date.dayOfMonth().withMaximumValue()).fetch();
			for(PersonDay p : pdList){
				if(p.absences.size() == 1 && p.absences.get(0).absenceType.code.equals("91")){
					compensatoryRest = compensatoryRest +1;					
				}
			}
			Competence comp = Competence.find("Select comp from Competence comp where comp.person = ? and comp.year = ? " +
					"and comp.month = ? and comp.competenceCode.description = ?", person, year, date.getMonthOfYear(), "S1").first();
			lista = new ArrayList<Integer>();
			if(pd != null){
				lista.add(0, pd.difference);
				lista.add(1, compensatoryRest);
				if(comp != null)
					lista.add(2, comp.valueApproved);
				else
					lista.add(2, 0);
				lista.add(3,pd.progressive);
				mapMonthSituation.put(date.getMonthOfYear(), lista);
			}	
			else{
				lista.add(0, 0);
				lista.add(1, 0);
				lista.add(2, 0);
				lista.add(3, 0);
				mapMonthSituation.put(date.getMonthOfYear(), lista);
			}
			compensatoryRest = 0;
			
		}
		
		render(mapMonthSituation, person, year);	
	}
}
