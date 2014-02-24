package controllers;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;

import models.Absence;
import models.Competence;
import models.Contract;
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
	
		if(params.get("yearChart") == null || params.get("monthChart") == null){
			Logger.debug("Params year: %s", params.get("yearChart", Integer.class));
			Logger.debug("Chiamato metodo con anno e mese nulli");
			render(annoList, meseList);
		}

		year = params.get("yearChart", Integer.class);
		month = params.get("monthChart", Integer.class);
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
	public static void overtimeOnPositiveResidualInYear(Integer year){
		List<Year> annoList = new ArrayList<Year>();
		annoList.add(new Year(1,2013));
		annoList.add(new Year(2,2014));
		annoList.add(new Year(3,2015));
		
		if(params.get("yearChart") == null && year == null){
			Logger.debug("Params year: %s", params.get("yearChart", Integer.class));
			Logger.debug("Chiamato metodo con anno e mese nulli");
			render(annoList);
		}
		year = params.get("yearChart", Integer.class);
		Logger.debug("Anno preso dai params: %d", year);
		Long val = Competence.find("Select sum(c.valueApproved) from Competence c where c.competenceCode.code in (?,?,?) and c.year = ?", 
				"S1","S2","S3", year).first();
		List<Person> personeProva = Person.getActivePersonsinYear(year, true);
		int totaleOreResidue = 0;
		for(Person p : personeProva){
			for(int month=1; month<13;month++){
				CalcoloSituazioneAnnualePersona sit = new CalcoloSituazioneAnnualePersona(p, year, new LocalDate(year,month,1).dayOfMonth().withMaximumValue());
				Mese mese = sit.getMese(year,month);
				totaleOreResidue = totaleOreResidue+(mese.positiveResidualInMonth(p, year, month)/60);
			}
			Logger.debug("Ore in più per %s %s nell'anno %d: %d", p.name, p.surname, year,totaleOreResidue);
		}
		
		render(annoList, val, totaleOreResidue);
		
	}
	
	@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void whichAbsenceInYear(Integer year){
		List<Year> annoList = new ArrayList<Year>();
		annoList.add(new Year(1,2013));
		annoList.add(new Year(2,2014));
		annoList.add(new Year(3,2015));
		
		if(params.get("yearChart") == null && year == null){
			Logger.debug("Params year: %s", params.get("yearChart", Integer.class));
			Logger.debug("Chiamato metodo con anno e mese nulli");
			render(annoList);
		}
		List<Absence> missioni = new ArrayList<Absence>();
		List<Absence> riposiCompensativi = new ArrayList<Absence>();
		List<Absence> malattia = new ArrayList<Absence>();
		List<Absence> altre = new ArrayList<Absence>();
		year = params.get("yearChart", Integer.class);
		Logger.debug("Anno preso dai params: %d", year);

		Long missioniSize = Absence.find("Select count(abs) from Absence abs where abs.personDay.date between ? and ? and abs.absenceType.code = ?", 
				new LocalDate(year,1,1), new LocalDate(year,1,1).monthOfYear().withMaximumValue().dayOfMonth().withMaximumValue(), "92").first();
		Long riposiCompensativiSize = Absence.find("Select count(abs) from Absence abs where abs.personDay.date between ? and ? and abs.absenceType.code = ?", 
				new LocalDate(year,1,1), new LocalDate(year,1,1).monthOfYear().withMaximumValue().dayOfMonth().withMaximumValue(), "91").first();
		Long malattiaSize = Absence.find("Select count(abs) from Absence abs where abs.personDay.date between ? and ? and abs.absenceType.code = ?", 
				new LocalDate(year,1,1), new LocalDate(year,1,1).monthOfYear().withMaximumValue().dayOfMonth().withMaximumValue(), "111").first();
		Long altreSize = Absence.find("Select count(abs) from Absence abs where abs.personDay.date between ? and ? and abs.absenceType.code not in(?,?,?)", 
				new LocalDate(year,1,1), new LocalDate(year,1,1).monthOfYear().withMaximumValue().dayOfMonth().withMaximumValue(), "92","91","111").first();

		//int missioniSize = missioni.size();
		//int riposiCompensativiSize = riposiCompensativi.size();
		//int malattiaSize = malattia.size();
		//int altreSize = altre.size();
		Logger.debug("Missioni size: %d", missioniSize);
		Logger.debug("RiposiCompensativi size: %d", riposiCompensativiSize);
		Logger.debug("Malattia size: %d", malattiaSize);
		Logger.debug("Altre size: %d", altreSize);
		
		render(annoList, missioniSize, riposiCompensativiSize, malattiaSize, altreSize);
		
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
