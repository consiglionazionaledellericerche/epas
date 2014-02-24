package models.rendering;

import models.Competence;
import models.CompetenceCode;
import models.Contract;
import models.Person;
import models.personalMonthSituation.CalcoloSituazioneAnnualePersona;
import models.personalMonthSituation.Mese;
import play.Logger;
import play.data.validation.Valid;

public class PersonMonthCompetenceRecap {
	public Person person;
	int year;
	int month;
	
	public int holidaysAvailability = 0;
	public int weekDayAvailability = 0;
	public int daylightWorkingDaysOvertime = 0;
	public int daylightholidaysOvertime = 0;
	public int ordinaryShift = 0;
	public int nightShift = 0;
	public int progressivoFinalePositivoMese;

	public PersonMonthCompetenceRecap(Person person, int month, int year) {
		this.person = person;
		this.year = year;
		this.month = month;
		
		this.holidaysAvailability = getHolidaysAvailability(person, year, month);
		this.weekDayAvailability = getWeekDayAvailability(person, year, month);
		this.daylightWorkingDaysOvertime = getDaylightWorkingDaysOvertime(person, year, month);
		this.daylightholidaysOvertime = getDaylightholidaysOvertime(person, year, month);
		this.ordinaryShift = getOrdinaryShift(person, year, month);
		this.nightShift = getNightShift(person, year, month);
		
		//RTODO
		Contract contract = person.getCurrentContract();
		CalcoloSituazioneAnnualePersona c = new CalcoloSituazioneAnnualePersona(contract, year, null);
		Mese mese = c.getMese(year, month);
		this.progressivoFinalePositivoMese = mese.progressivoFinalePositivoMese;
	}

	/**
	 * Ritorna il numero di giorni di indennità di reperibilità festiva per la persona nel mese.
	 * @param person
	 * @param year
	 * @param month
	 * @return
	 */
	private static int getHolidaysAvailability(Person person, int year, int month){
		int holidaysAvailability = 0;
		CompetenceCode cmpCode = CompetenceCode.find("Select cmp from CompetenceCode cmp where cmp.code = ?", "208").first();
		Competence competence = Competence.find("Select comp from Competence comp, CompetenceCode cmpCode where comp.person = ? and " +
				"comp.year = ? and comp.month = ? and comp.competenceCode = cmpCode and cmpCode = ?", person, year, month, cmpCode).first();
		if(competence != null)
			holidaysAvailability = competence.valueApproved;
		else
			holidaysAvailability = 0;
		return holidaysAvailability;
	}

	/**
	 * Ritorna il numero di giorni di indennità di reperibilità feriale per la persona nel mese
	 * @param person
	 * @param year
	 * @param month
	 * @return
	 */
	private static int getWeekDayAvailability(Person person, @Valid int year, @Valid int month){
		int weekDayAvailability = 0;
		CompetenceCode cmpCode = CompetenceCode.find("Select cmp from CompetenceCode cmp where cmp.code = ?", "207").first();
		Logger.debug("Il codice competenza é: %s", cmpCode);

		Competence competence = Competence.find("Select comp from Competence comp, CompetenceCode cmpCode where comp.person = ? and " +
				"comp.year = ? and comp.month = ? and comp.competenceCode = cmpCode and cmpCode = ?", person, year, month, cmpCode).first();
		if(competence != null)
			weekDayAvailability = competence.valueApproved;
		else
			weekDayAvailability = 0;
		return weekDayAvailability;
	}

	/**
	 * Ritorna il numero di giorni di straordinario diurno nei giorni lavorativi per la persona nel mese
	 * @param person
	 * @param year
	 * @param month
	 * @return
	 */
	private static int getDaylightWorkingDaysOvertime(Person person, int year, int month){
		int daylightWorkingDaysOvertime = 0;
		CompetenceCode cmpCode = CompetenceCode.find("Select cmp from CompetenceCode cmp where cmp.code = ?", "S1").first();
		Logger.debug("Il codice competenza é: %s", cmpCode);
		Competence competence = Competence.find("Select comp from Competence comp, CompetenceCode cmpCode where comp.person = ? and " +
				"comp.year = ? and comp.month = ? and comp.competenceCode = cmpCode and cmpCode = ?", person, year, month, cmpCode).first();
		if(competence != null)
			daylightWorkingDaysOvertime = competence.valueApproved;
		else
			daylightWorkingDaysOvertime = 0;
		return daylightWorkingDaysOvertime;
	}

	/**
	 * Ritorna il numero di giorni di straordinario diurno nei giorni festivi o notturno nei giorni lavorativi per la persona nel mese
	 * @param person
	 * @param year
	 * @param month
	 * @return
	 */
	private static int getDaylightholidaysOvertime(Person person, int year, int month){
		int daylightholidaysOvertime = 0;
		CompetenceCode cmpCode = CompetenceCode.find("Select cmp from CompetenceCode cmp where cmp.code = ?", "S2").first();
		Logger.debug("Il codice competenza é: %s", cmpCode);
		Competence competence = Competence.find("Select comp from Competence comp, CompetenceCode cmpCode where comp.person = ? and " +
				"comp.year = ? and comp.month = ? and comp.competenceCode = cmpCode and cmpCode = ?", person, year, month, cmpCode).first();
		if(competence != null)
			daylightholidaysOvertime = competence.valueApproved;
		else
			daylightholidaysOvertime = 0;
		return daylightholidaysOvertime;
	}

	/**
	 * Ritorna il numero di giorni di turno ordinario per la persona nel mese
	 * @param person
	 * @param year
	 * @param month
	 * @return
	 */
	private static int getOrdinaryShift(Person person, int year, int month){
		int ordinaryShift = 0;
		CompetenceCode cmpCode = CompetenceCode.find("Select cmp from CompetenceCode cmp where cmp.code = ?", "T1").first();
		Logger.debug("Il codice competenza é: %s", cmpCode);
		Competence competence = Competence.find("Select comp from Competence comp, CompetenceCode cmpCode where comp.person = ? and " +
				"comp.year = ? and comp.month = ? and comp.competenceCode = cmpCode and cmpCode = ?", person, year, month, cmpCode).first();
		if(competence != null)
			ordinaryShift = competence.valueApproved;
		else
			ordinaryShift = 0;
		return ordinaryShift;
	}

	/**
	 * Ritorna il numero di giorni di turno notturno per la persona nel mese
	 * @param person
	 * @param year
	 * @param month
	 * @return
	 */
	private static int getNightShift(Person person, int year, int month){
		int nightShift = 0;
		CompetenceCode cmpCode = CompetenceCode.find("Select cmp from CompetenceCode cmp where cmp.code = ?", "T2").first();
		Logger.debug("Il codice competenza é: %s", cmpCode);
		if(cmpCode == null)
			return 0;
		Competence competence = Competence.find("Select comp from Competence comp, CompetenceCode cmpCode where comp.person = ? and " +
				"comp.year = ? and comp.month = ? and comp.competenceCode = cmpCode and cmpCode = ?", person, year, month, cmpCode).first();
		if(competence != null)
			nightShift = competence.valueApproved;
		else
			nightShift = 0;
		return nightShift;
	}
}
