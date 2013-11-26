package controllers;

import it.cnr.iit.epas.DateUtility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.LocalDate;

import models.Absence;
import models.Competence;
import models.CompetenceCode;
import models.InitializationTime;
import models.Person;
import models.PersonDay;
import models.PersonMonth;
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
		Integer compensatoryRest = 0;
		
		CalcoloSituazioneAnnualePersona c = null;
		InitializationTime initializationTime = InitializationTime.find("Select i from InitializationTime i where i.person = ?" , person).first();
		if(initializationTime == null)
			c = new CalcoloSituazioneAnnualePersona(person, 2013, 0);
		else
			c = new CalcoloSituazioneAnnualePersona(person, 2013, initializationTime.residualMinutesPastYear);
		
		for(int month = 1; month < 13; month++){
			Mese mese = c.getMese(year, month);
			LocalDate date = new LocalDate(year, month, 1);
			/**
			 * cerco il personMonth relativo al mese considerato per trovare i dati di cui avrò bisogno in fondo al metodo da passare alla
			 * mia form (residui anno precedente, mese precedente ecc...)
			 */
			PersonMonth pm = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and pm.year = ? and pm.month = ?", 
					person, year, month).first();
			Logger.debug("PersonMonth = %s", pm);



			/**
			 * cerco nel mese eventuali giorni di riposo compensativo
			 */
//			List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?", 
//					person, date, date.dayOfMonth().withMaximumValue()).fetch();
//			for(PersonDay p : pdList){
//				if(p.absences.size() > 0 && p.absences.get(0).absenceType.code.equals("91")){
//					compensatoryRest = compensatoryRest +1;					
//				}
//			}
			/**
			 * cerco nelle competenze del mese eventuali straordinari
			 */
//			
//			List<Competence> compList = Competence.find("Select comp from Competence comp where comp.person = ? and comp.year = ? " +
//					"and comp.month = ? ", person, year, date.getMonthOfYear()).fetch();
//			Logger.debug("La competenza sugli straordinari per il %s mese è: %s", month, compList.toString());

			/**
			 * aggiungo tutti gli elementi alla lista
			 */
			lista = new ArrayList<String>();
			//if(pm != null && pm.possibileUtilizzareResiduoAnnoPrecedente() && pm.residuoAnnoPrecedenteDaInizializzazione() != 0)
			
			
			//else
				//lista.add(0, 0);
//			if(month == 1)
//				lista.add(1, 0);
//			else
//				lista.add(1, mese.mesePrecedente.monteOreAnnoPassato+mese.mesePrecedente.monteOreAnnoCorrente);
			if(mese.mese != 1){
				lista.add(0, 0+"");
				lista.add(1, DateUtility.fromMinuteToHourMinute(mese.mesePrecedente.monteOreAnnoCorrente));
				lista.add(2, DateUtility.fromMinuteToHourMinute(mese.mesePrecedente.monteOreAnnoPassato));
				lista.add(3, DateUtility.fromMinuteToHourMinute(mese.mesePrecedente.monteOreAnnoPassato + mese.mesePrecedente.monteOreAnnoCorrente));
			}
			else{
				lista.add(0, DateUtility.fromMinuteToHourMinute(mese.tempoInizializzazione));
				lista.add(1, 0+"");
				lista.add(2, 0+"");
				lista.add(3, DateUtility.fromMinuteToHourMinute(mese.tempoInizializzazione));
			}			
			lista.add(4, DateUtility.fromMinuteToHourMinute(mese.progressivoFinaleMese));
			Integer minutiRiposiCompensativi = mese.riposiCompensativiMinutiImputatoAnnoCorrente + mese.riposiCompensativiMinutiImputatoAnnoPassato + mese.riposiCompensativiMinutiImputatoProgressivoFinalePositivoMese; 
			Integer riposiCompensativi = minutiRiposiCompensativi/mese.workingTime;
			lista.add(5, riposiCompensativi + " ("+ DateUtility.fromMinuteToHourMinute(minutiRiposiCompensativi)+")");
			lista.add(6, mese.straordinariMinuti /60+"");
			lista.add(7, DateUtility.fromMinuteToHourMinute(mese.monteOreAnnoCorrente + mese.monteOreAnnoPassato));
						
//			
//			else{
//				if(pm != null){
//					/**
//					 * TODO: qui va messo le somme dei residui dei mesi precedenti - le somme degli straordinari assegnati i mesi precedenti
//					 */
//					int situazioneParziale = 0;
//
//					PersonMonth count = pm;
//					while(count.month > 1){
//						situazioneParziale = situazioneParziale + count.mesePrecedente().residuoDelMese() - count.mesePrecedente().straordinari;
//						count = count.mesePrecedente();
//					}
//					lista.add(1, situazioneParziale);
//				}
//
//				else
//					lista.add(1, 0);
//			}
//			if((month < 4 || person.qualification.qualification < 4) && pm != null && pm.mesePrecedente() != null)
//				lista.add(2, pm.residuoAnnoPrecedente() + pm.mesePrecedente().riposiCompensativiDaAnnoPrecedente);
//			else
//				lista.add(2, 0);
//			if(pm != null){
//				if(month == 1)
//					lista.add(3, 0+pm.residuoAnnoPrecedente());
//
//				else{
//					if(month < 4 || person.qualification.qualification < 4)
//						lista.add(3, pm.mesePrecedente().totaleResiduoAnnoCorrenteAFineMesePiuResiduoAnnoPrecedenteDisponibileAFineMese());
//					else
//						/**
//						 * TODO: da sistemare che ci vuole il valore che c'è nel campo mese precedente dal mese di aprile in poi per i tecnici 
//						 */{
//						int situazioneParziale = 0;
//
//						PersonMonth count = pm;
//						while(count.month > 1){
//							situazioneParziale = situazioneParziale + count.mesePrecedente().residuoDelMese() - count.mesePrecedente().straordinari;
//							count = count.mesePrecedente();
//						}
//						lista.add(3, situazioneParziale);
//					}
//				}
//
//			}
//			else{
//				lista.add(3, 0);
//			}
//			//			if(pd != null){
//			int valoreStraordinari = 0;
//			if(pm != null)
//				lista.add(4, pm.residuoDelMese());
//			else
//				lista.add(4, 0);
//			lista.add(5, compensatoryRest);
//
//			for(Competence comp : compList){
//				if(comp.competenceCode.code.equals("S1") || comp.competenceCode.code.equals("S2") || comp.competenceCode.code.equals("S3"))
//					valoreStraordinari = valoreStraordinari + comp.valueApproved;
//
//				else
//					lista.add(6, 0);
//			}
//			lista.add(6, valoreStraordinari);
//			Logger.debug("Aggiunto alla lista il valore %d per gli straordinari del mese di %d", valoreStraordinari, month);
//
//			if(pm != null)
//				lista.add(7,pm.totaleResiduoAnnoCorrenteAFineMesePiuResiduoAnnoPrecedenteDisponibileAFineMese());
//			else
//				lista.add(7, 0);
			mapMonthSituation.put(date.getMonthOfYear(), lista);
	
//			compensatoryRest = 0;

		}

		render(mapMonthSituation, person, year);	
	}
}
