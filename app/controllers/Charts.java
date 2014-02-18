package controllers;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;

import models.Competence;
import models.Person;
import models.exports.PersonOvertime;
import models.personalMonthSituation.CalcoloSituazioneAnnualePersona;
import models.personalMonthSituation.Mese;
import play.Logger;
import play.mvc.Controller;
import play.mvc.With;

@With( {Secure.class, NavigationMenu.class} )
public class Charts extends Controller{

	@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void overtimeOnPositiveResidual(Integer year, Integer month){
		
		List<Year> annoList = new ArrayList<Year>();
		annoList.add(new Year(1,2013));
		annoList.add(new Year(2,2014));
		annoList.add(new Year(3,2015));
		
		List<Month> meseList = new ArrayList<Month>();

		meseList.add(new Month(1,"Gennaio"));
		meseList.add(new Month(2,"Febbraio"));
		meseList.add(new Month(3,"Marzo"));
		meseList.add(new Month(4,"Aprile"));
		meseList.add(new Month(5,"Maggio"));
		meseList.add(new Month(6,"Giugno"));
		meseList.add(new Month(7,"Luglio"));
		meseList.add(new Month(8,"Agosto"));
		meseList.add(new Month(9,"Settembre"));
		meseList.add(new Month(10,"Ottobre"));
		meseList.add(new Month(11,"Novembre"));
		meseList.add(new Month(12,"Dicembre"));
		/**
		 * TODO: per adesso, per prova, anno e mese sono statici, a regime dovranno essere passati come parametro al controller
		 */
		if(year == null || month == null){
			render(annoList, meseList);
		}

		Logger.debug("Anno da params: %s, Mese da params: %s", params.get("yearChart"), params.get("monthChart"));
		List<Person> personeProva = Person.getActivePersonsInMonth(month, year, true);
		List<PersonOvertime> poList = new ArrayList<PersonOvertime>();
		for(Person p : personeProva){
			PersonOvertime po = new PersonOvertime();

			Long val = Competence.find("Select sum(c.valueApproved) from Competence c where c.competenceCode.code in (?,?,?) and c.year = ? and c.month = ? and c.person = ?",
					"S1","S2","S3", year, month, p).first();

			CalcoloSituazioneAnnualePersona sit = new CalcoloSituazioneAnnualePersona(p, year, new LocalDate(year,month,1));
			Mese mese = sit.getMese(year,month);
			po.month = 1;
			po.year = 2013;
			po.overtimeHour = val;
			po.name = p.name;
			po.surname = p.surname;
			po.positiveHourForOvertime = mese.positiveResidualInMonth(p, year, month)/60;
			poList.add(po);
		}
		render(poList, year, month, annoList, meseList);
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

	@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void overtimeOnPositiveResidualInYear(int year){

	}

	private static class Month{
		private int id;
		private String mese;

		private Month(int id, String mese){
			this.id = id;
			this.mese = mese;
		}
	}
	
	private static class Year{
		private int id;
		private int anno;

		private Year(int id, int anno){
			this.id = id;
			this.anno = anno;
		}
	}
}
