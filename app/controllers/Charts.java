package controllers;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;

import models.Competence;
import models.Contract;
import models.Person;
import models.exports.PersonOvertime;
import models.personalMonthSituation.CalcoloSituazioneAnnualePersona;
import models.personalMonthSituation.Mese;
import play.mvc.Controller;
import play.mvc.With;

@With( {Secure.class, NavigationMenu.class} )
public class Charts extends Controller{

	@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void overtimeOnPositiveResidual(){
		/**
		 * TODO: per adesso, per prova, anno e mese sono statici, a regime dovranno essere passati come parametro al controller
		 */
		int year = 2013;
		int month = 1;
		List<Person> personeProva = Person.getActivePersonsInMonth(month, year, true);
		List<PersonOvertime> poList = new ArrayList<PersonOvertime>();
		for(Person p : personeProva){
			PersonOvertime po = new PersonOvertime();
			
			Long val = Competence.find("Select sum(c.valueApproved) from Competence c where c.competenceCode.code in (?,?,?) and c.year = ? and c.month = ? and c.person = ?",
					"S1","S2","S3", year, month, p).first();

			//RTODO contratto attivo??
			Contract contract = p.getCurrentContract();
			CalcoloSituazioneAnnualePersona sit = new CalcoloSituazioneAnnualePersona(contract, year, new LocalDate(year,month,1));
			Mese mese = sit.getMese(year,month);
			po.month = 1;
			po.year = 2013;
			po.overtimeHour = val;
			po.name = p.name;
			po.surname = p.surname;
			po.positiveHourForOvertime = mese.positiveResidualInMonth(p, year, month)/60;
			poList.add(po);
		}
		render(poList, year, month);
	}
	
	public static void compensatoryRestInYear(){
		int year = 2013;
		List<Person> personeProva = Person.getActivePersonsinYear(year, true);
		for(Person p : personeProva){
			
		}
	}
	
	@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void indexCharts(){
		render();
	}
	


}
