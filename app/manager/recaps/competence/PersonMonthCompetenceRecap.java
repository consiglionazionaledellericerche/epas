package manager.recaps.competence;

import manager.recaps.residual.PersonResidualMonthRecap;
import manager.recaps.residual.PersonResidualYearRecap;
import manager.recaps.residual.PersonResidualYearRecapFactory;
import models.Competence;
import models.CompetenceCode;
import models.Contract;
import models.Person;
import play.Logger;
import play.data.validation.Valid;

import com.google.common.base.Optional;

import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.ContractDao;

/**
 * Riepilogo che popola la vista competenze del dipendente.
 *
 */
public class PersonMonthCompetenceRecap {
	
	private CompetenceCodeDao competenceCodeDao;
	private CompetenceDao competenceDao;	
	
	public Person person;
	public int year;
	public int month;
	
	public int holidaysAvailability = 0;
	public int weekDayAvailability = 0;
	public int daylightWorkingDaysOvertime = 0;
	public int daylightholidaysOvertime = 0;
	public int ordinaryShift = 0;
	public int nightShift = 0;
	public int progressivoFinalePositivoMese;


	public PersonMonthCompetenceRecap(CompetenceCodeDao competenceCodeDao,
			CompetenceDao competenceDao, PersonResidualYearRecapFactory yearFactory,
			Person person, int month, int year) {
		
		this.competenceCodeDao = competenceCodeDao;
		this.competenceDao = competenceDao;
		
		this.person = person;
		this.year = year;
		this.month = month;
		
		//TODO implementare dei metodi un pò più generali (con enum come parametro)
		this.holidaysAvailability = getHolidaysAvailability(person, year, month);
		this.weekDayAvailability = getWeekDayAvailability(person, year, month);
		this.daylightWorkingDaysOvertime = getDaylightWorkingDaysOvertime(person, year, month);
		this.daylightholidaysOvertime = getDaylightholidaysOvertime(person, year, month);
		this.ordinaryShift = getOrdinaryShift(person, year, month);
		this.nightShift = getNightShift(person, year, month);
		
		Contract contract = ContractDao.getCurrentContract(person);
			
		PersonResidualYearRecap c = 
				yearFactory.create(contract, year, null);
		PersonResidualMonthRecap mese = c.getMese(month);
		
		this.progressivoFinalePositivoMese = mese.progressivoFinalePositivoMese;
		
	}
	
	

	/**
	 * Ritorna il numero di giorni di indennità di reperibilità festiva per la persona nel mese.
	 * @param person
	 * @param year
	 * @param month
	 * @return
	 */
	private int getHolidaysAvailability(Person person, int year, int month){
		int holidaysAvailability = 0;
		CompetenceCode cmpCode = competenceCodeDao.getCompetenceCodeByCode("208");

		Optional<Competence> competence = competenceDao.getCompetence(person, year, month, cmpCode);

		if(competence.isPresent())
			holidaysAvailability = competence.get().valueApproved;
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
	private int getWeekDayAvailability(Person person, @Valid int year, @Valid int month){
		int weekDayAvailability = 0;
		CompetenceCode cmpCode = competenceCodeDao.getCompetenceCodeByCode("207");

		Logger.debug("Il codice competenza é: %s", cmpCode);
		Optional<Competence> competence = competenceDao.getCompetence(person, year, month, cmpCode);

		if(competence.isPresent())
			weekDayAvailability = competence.get().valueApproved;
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
	private int getDaylightWorkingDaysOvertime(Person person, int year, int month){
		int daylightWorkingDaysOvertime = 0;
		CompetenceCode cmpCode = competenceCodeDao.getCompetenceCodeByCode("S1");

		Logger.debug("Il codice competenza é: %s", cmpCode);
		Optional<Competence> competence = competenceDao.getCompetence(person, year, month, cmpCode);

		if(competence.isPresent())
			daylightWorkingDaysOvertime = competence.get().valueApproved;
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
	private int getDaylightholidaysOvertime(Person person, int year, int month){
		int daylightholidaysOvertime = 0;
		CompetenceCode cmpCode = competenceCodeDao.getCompetenceCodeByCode("S2");
		Logger.debug("Il codice competenza é: %s", cmpCode);
		Optional<Competence> competence = competenceDao.getCompetence(person, year, month, cmpCode); 

		if(competence.isPresent())
			daylightholidaysOvertime = competence.get().valueApproved;
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
	private int getOrdinaryShift(Person person, int year, int month){
		int ordinaryShift = 0;
		CompetenceCode cmpCode = competenceCodeDao.getCompetenceCodeByCode("T1");

		Logger.debug("Il codice competenza é: %s", cmpCode);
		Optional<Competence> competence = competenceDao.getCompetence(person, year, month, cmpCode);

		if(competence.isPresent())
			ordinaryShift = competence.get().valueApproved;
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
	private int getNightShift(Person person, int year, int month){
		int nightShift = 0;
		CompetenceCode cmpCode = competenceCodeDao.getCompetenceCodeByCode("T2");

		Logger.debug("Il codice competenza é: %s", cmpCode);
		if(cmpCode == null)
			return 0;
		Optional<Competence> competence = competenceDao.getCompetence(person, year, month, cmpCode);

		if(competence.isPresent())
			nightShift = competence.get().valueApproved;
		else
			nightShift = 0;
		return nightShift;
	}
}
