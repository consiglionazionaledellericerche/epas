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
import models.Contract;
import models.InitializationTime;
import models.Person;
import models.PersonDay;
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
				
		//RTODO
		Contract contract = person.getCurrentContract();
		CalcoloSituazioneAnnualePersona c = new CalcoloSituazioneAnnualePersona(contract, year, null);
		
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
}
