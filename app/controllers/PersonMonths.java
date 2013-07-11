package controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.LocalDate;

import models.Absence;
import models.Competence;
import models.CompetenceCode;
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


			//			PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", 
			//					person, date.dayOfMonth().withMaximumValue()).first();
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
			//CompetenceCode code = CompetenceCode.find("Select code from CompetenceCode code where code.description = ?", "S1").first();
			List<Competence> compList = Competence.find("Select comp from Competence comp where comp.person = ? and comp.year = ? " +
					"and comp.month = ? ", person, year, date.getMonthOfYear()).fetch();
			Logger.debug("La competenza sugli straordinari per il %s mese è: %s", month, compList.toString());

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
				if(pm != null){
					/**
					 * TODO: qui va messo le somme dei residui dei mesi precedenti - le somme degli straordinari assegnati i mesi precedenti
					 */
					int situazioneParziale = 0;

					PersonMonth count = pm;
					while(count.month > 1){
						situazioneParziale = situazioneParziale + count.mesePrecedente().residuoDelMese() - count.mesePrecedente().straordinari;
						count = count.mesePrecedente();
					}
					lista.add(1, situazioneParziale);
				}

				else
					lista.add(1, 0);
			}
			if((month < 4 || person.qualification.qualification < 4) && pm != null && pm.mesePrecedente() != null)
				lista.add(2, pm.residuoAnnoPrecedente() + pm.mesePrecedente().riposiCompensativiDaAnnoPrecedente);
			else
				lista.add(2, 0);
			if(pm != null){
				if(month == 1)
					lista.add(3, 0+pm.residuoAnnoPrecedente());

				else{
					if(month < 4 || person.qualification.qualification < 4)
						lista.add(3, pm.mesePrecedente().totaleResiduoAnnoCorrenteAFineMesePiuResiduoAnnoPrecedenteDisponibileAFineMese());
					else
						/**
						 * TODO: da sistemare che ci vuole il valore che c'è nel campo mese precedente dal mese di aprile in poi per i tecnici 
						 */{
						int situazioneParziale = 0;

						PersonMonth count = pm;
						while(count.month > 1){
							situazioneParziale = situazioneParziale + count.mesePrecedente().residuoDelMese() - count.mesePrecedente().straordinari;
							count = count.mesePrecedente();
						}
						lista.add(3, situazioneParziale);
					}
				}

			}
			else{
				lista.add(3, 0);
			}
			//			if(pd != null){
			int valoreStraordinari = 0;
			if(pm != null)
				lista.add(4, pm.residuoDelMese());
			else
				lista.add(4, 0);
			lista.add(5, compensatoryRest);

			//				if(comp.valueApproved != 0)					
			//					lista.add(6, comp.valueApproved);									
			//				else
			//					lista.add(6, 0);
			for(Competence comp : compList){
				if(comp.competenceCode.code.equals("S1") || comp.competenceCode.code.equals("S2") || comp.competenceCode.code.equals("S3"))
					valoreStraordinari = valoreStraordinari + comp.valueApproved;

				else
					lista.add(6, 0);
			}
			lista.add(6, valoreStraordinari);
			Logger.debug("Aggiunto alla lista il valore %d per gli straordinari del mese di %d", valoreStraordinari, month);

			if(pm != null)
				lista.add(7,pm.totaleResiduoAnnoCorrenteAFineMesePiuResiduoAnnoPrecedenteDisponibileAFineMese());
			else
				lista.add(7, 0);
			mapMonthSituation.put(date.getMonthOfYear(), lista);
			//			}	
			//			else{
			//				lista.add(4, 0);
			//				lista.add(5, 0);
			//				lista.add(6, 0);
			//				lista.add(7, 0);
			//				mapMonthSituation.put(date.getMonthOfYear(), lista);
			//			}
			compensatoryRest = 0;

		}

		render(mapMonthSituation, person, year);	
	}
}
