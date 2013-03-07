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
import models.PersonMonth;
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
			/**
			 * cerco il personMonth relativo al mese considerato per trovare i dati di cui avrò bisogno in fondo al metodo da passare alla
			 * mia form (residui anno precedente, mese precedente ecc...)
			 */
			PersonMonth pm = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and pm.year = ? and pm.month = ?", 
					person, year, month).first();
			Logger.debug("PersonMonth = %s", pm);
			
			
			PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", 
					person, date.dayOfMonth().withMaximumValue()).first();
			//Logger.debug("Il person day scansionato è: %s", pd);
			
			/**
			 * cerco nel mese eventuali giorni di riposo compensativo
			 */
			List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?", 
					person, date, date.dayOfMonth().withMaximumValue()).fetch();
			for(PersonDay p : pdList){
				if(p.absences.size() > 0 && p.absences.get(0).absenceType.code.equals("91")){
					compensatoryRest = compensatoryRest +1;					
				}
			}
			/**
			 * cerco nelle competenze del mese eventuali straordinari
			 */
			Competence comp = Competence.find("Select comp from Competence comp where comp.person = ? and comp.year = ? " +
					"and comp.month = ? and comp.competenceCode.description = ?", person, year, date.getMonthOfYear(), "S1").first();
			
			/**
			 * aggiungo tutti gli elementi alla lista
			 */
			lista = new ArrayList<Integer>();
			if(pm != null && pm.possibileUtilizzareResiduoAnnoPrecedente() && pm.residuoAnnoPrecedenteDaInizializzazione() != 0)
				lista.add(0, pm.residuoAnnoPrecedenteDaInizializzazione());
			else
				lista.add(0, 0);
			if(month == 1)
				lista.add(1, pm.residuoAnnoCorrenteDaInizializzazione());
			else{
				if(pm != null)
					lista.add(1, pm.mesePrecedente().residuoDelMese());
				else
					lista.add(1, 0);
			}
			if((month < 4 || person.qualification.qualification < 4) && pm != null)
				lista.add(2, pm.residuoAnnoPrecedente());
			else
				lista.add(2, 0);
			if(pm != null){
				if(month == 1)
					lista.add(3, 0+pm.residuoAnnoPrecedente());
				else
					lista.add(3, pm.mesePrecedente().residuoDelMese()+pm.residuoAnnoPrecedente()+pm.residuoAnnoPrecedenteDaInizializzazione());
			}
			else{
				lista.add(3, 0);
			}
			if(pd != null){
				if(pm != null)
					lista.add(4, pm.residuoDelMese());
				else
					lista.add(4, 0);
				lista.add(5, compensatoryRest);
				if(comp != null)
					lista.add(6, comp.valueApproved);
				else
					lista.add(6, 0);
				if(pm != null)
					lista.add(7,pm.totaleResiduoAnnoCorrenteAFineMesePiuResiduoAnnoPrecedenteDisponibileAFineMese());
				else
					lista.add(7, 0);
				mapMonthSituation.put(date.getMonthOfYear(), lista);
			}	
			else{
				lista.add(4, 0);
				lista.add(5, 0);
				lista.add(6, 0);
				lista.add(7, 0);
				mapMonthSituation.put(date.getMonthOfYear(), lista);
			}
			compensatoryRest = 0;
			
		}
		
		render(mapMonthSituation, person, year);	
	}
}
