package controllers;

import it.cnr.iit.epas.DateUtility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.envers.reader.FirstLevelCache;
import org.joda.time.LocalDate;

import models.Absence;
import models.Competence;
import models.CompetenceCode;
import models.InitializationTime;
import models.Person;
import models.PersonDay;
import models.PersonMonthRecap;
import models.personalMonthSituation.CalcoloSituazioneAnnualePersona;
import models.personalMonthSituation.Mese;
import play.mvc.Controller;
import play.mvc.With;
import play.Logger;

@With( {Secure.class, NavigationMenu.class} )
public class PersonMonths extends Controller{

	@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void hourRecap(Long personId,int year){
		
		if(year > new LocalDate().getYear()){
			flash.error("Richiesto riepilogo orario di un anno futuro. Impossibile soddisfare la richiesta");
			renderTemplate("Application/indexAdmin.html");
		}
		Person person = null;
		if(personId != null)
			person = Person.findById(personId);
		else
			person = Security.getPerson();
		Map<Integer, List<String>> mapMonthSituation = new HashMap<Integer, List<String>>();
		List<String> lista = null;
				
		CalcoloSituazioneAnnualePersona c = new CalcoloSituazioneAnnualePersona(person, year, null);
		
		int firstYear = 2013;	//TODO provvisorio fin quando non verranno persistiti i valori iniziali
		
		
		for(int month = 1; month < 13; month++){
			Mese mese = c.getMese(year, month);
			LocalDate date = new LocalDate(year, month, 1);
			lista = new ArrayList<String>();
			if(mese.mese != 1){
				lista.add(0, 0+"");
				lista.add(1, DateUtility.fromMinuteToHourMinute(mese.mesePrecedente.monteOreAnnoCorrente));
				lista.add(2, DateUtility.fromMinuteToHourMinute(mese.mesePrecedente.monteOreAnnoPassato));
				lista.add(3, DateUtility.fromMinuteToHourMinute(mese.mesePrecedente.monteOreAnnoPassato + mese.mesePrecedente.monteOreAnnoCorrente));
			}
			else{
				if(year==firstYear)
				{
					lista.add(0, DateUtility.fromMinuteToHourMinute(mese.tempoInizializzazione));
					lista.add(1, 0+"");
					lista.add(2, 0+"");
					lista.add(3, DateUtility.fromMinuteToHourMinute(mese.tempoInizializzazione));
				}
				else
				{
					lista.add(0, 0+"");
					lista.add(1, 0+"");
					lista.add(2, DateUtility.fromMinuteToHourMinute(mese.tempoInizializzazione));
					lista.add(3, DateUtility.fromMinuteToHourMinute(mese.tempoInizializzazione));
				}
			}			
			lista.add(4, DateUtility.fromMinuteToHourMinute(mese.progressivoFinaleMese));
			Integer minutiRiposiCompensativi = mese.riposiCompensativiMinutiImputatoAnnoCorrente + mese.riposiCompensativiMinutiImputatoAnnoPassato + mese.riposiCompensativiMinutiImputatoProgressivoFinalePositivoMese; 
			//Integer riposiCompensativi = minutiRiposiCompensativi/mese.workingTime;
			lista.add(5, mese.numeroRiposiCompensativi + " ("+ DateUtility.fromMinuteToHourMinute(minutiRiposiCompensativi)+")");
			lista.add(6, mese.straordinariMinuti /60+"");
			lista.add(7, DateUtility.fromMinuteToHourMinute(mese.monteOreAnnoCorrente + mese.monteOreAnnoPassato));
			mapMonthSituation.put(date.getMonthOfYear(), lista);
		}

		render(mapMonthSituation, person, year);	
	}
	
	@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void trainingHours(Long personId, int year, int month){
		Person person = Person.findById(personId);
		Logger.debug("Ore di formazione per %s %s nel mese %d dell'anno %d", person.name, person.surname, month, year);
		List<PersonMonthRecap> pmList = null;
		pmList = PersonMonthRecap.find("Select pm from PersonMonthRecap pm where pm.year = ? and pm.person = ?", year, person).fetch();
		
		if(pmList == null || pmList.size() == 0){
			Logger.debug("Lista nulla, la popolo");
			pmList = new ArrayList<PersonMonthRecap>();
			for(int i=1; i< 13; i++){
				PersonMonthRecap pm = new PersonMonthRecap(person, year, i);
				pm.trainingHours = 0;
				pm.hoursApproved = false;
				pm.save();
				pmList.add(new PersonMonthRecap(person,year,i));
				Logger.debug("Aggiunto elemento alla lista dei person month recap con valore del mese %d", i);
			}
		}
		render(person, year, month, pmList);
		
	}
	
	@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void insertTrainingHours(long pk, Integer value){
		
		
		PersonMonthRecap pm = PersonMonthRecap.findById(pk);
//		if(pm == null)
//			pm = new PersonMonthRecap(person, year, month);
		Logger.debug("Recuperato person month recap con valori: %s %s %s", pm.person, pm.year, pm.month);
		pm.trainingHours = value;
		pm.hoursApproved = false;
		pm.save();
		flash.success("Aggiornato il valore per le ore di formazione");
		Stampings.stampings(pm.year, pm.month);
	}
}
