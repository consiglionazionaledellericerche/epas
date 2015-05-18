package manager.recaps.residual;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.List;

import models.Absence;
import models.Competence;
import models.CompetenceCode;
import models.ConfYear;
import models.Contract;
import models.Person;
import models.PersonDay;
import models.enumerate.JustifiedTimeAtWork;
import models.enumerate.Parameter;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;

import dao.AbsenceDao;
import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.ConfYearDao;
import dao.MealTicketDao;
import dao.PersonDayDao;
import dao.WorkingTimeTypeDayDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;

public class PersonResidualMonthRecap {

	private final IWrapperFactory wrapperFactory; 
	private final AbsenceDao absenceDao;
	private final MealTicketDao mealTicketDao;
	private final PersonDayDao personDayDao;
	private final CompetenceDao competenceDao;
	private final DateUtility dateUtility;
	private final ConfYearDao confYearDao;
	private final CompetenceCodeDao competenceCodeDao;
	private final WorkingTimeTypeDayDao workingTimeTypeDayDao;

	public final IWrapperContract contract;

	public Person person;
	public String contractDescription;
	public PersonResidualMonthRecap mesePrecedente;
	public int qualifica;
	public boolean possibileUtilizzareResiduoAnnoPrecedente = true;
	public final int anno;
	public final int mese;

	public int initMonteOreAnnoPassato;
	public int initMonteOreAnnoCorrente;

	public int initResiduoAnnoCorrenteNelMese = 0;	//per il template (se sourceContract è del mese)

	public int progressivoFinaleMese		 = 0;	//person day
	public int progressivoFinalePositivoMese = 0;	//person day
	public int progressivoFinaleNegativoMese = 0;	//person day

	public int progressivoFinalePositivoMesePrint = 0;	//per il template

	public int straordinariMinuti 			 = 0;	//competences
	public int straordinariMinutiS1Print	 = 0;	//per il template
	public int straordinariMinutiS2Print	 = 0;	//per il template
	public int straordinariMinutiS3Print	 = 0;	//per il template

	public int riposiCompensativiMinuti 	 = 0;	//absences 
	public int riposiCompensativiMinutiPrint = 0;	//per il template


	public int progressivoFinaleNegativoMeseImputatoAnnoPassato;
	public int progressivoFinaleNegativoMeseImputatoAnnoCorrente;
	public int progressivoFinaleNegativoMeseImputatoProgressivoFinalePositivoMese;

	public int riposiCompensativiMinutiImputatoAnnoPassato;
	public int riposiCompensativiMinutiImputatoAnnoCorrente;
	public int riposiCompensativiMinutiImputatoProgressivoFinalePositivoMese;

	public int monteOreAnnoPassato;
	public int monteOreAnnoCorrente;
	public int numeroRiposiCompensativi;

	public int oreLavorate = 0;

	public int buoniPastoDalMesePrecedente = 0;
	public int buoniPastoConsegnatiNelMese = 0;
	public int buoniPastoUsatiNelMese = 0;
	public int buoniPastoResidui = 0;


	public PersonResidualMonthRecap(AbsenceDao absenceDao, PersonDayDao personDayDao,
			MealTicketDao mealTicketDao,CompetenceDao competenceDao,
			IWrapperFactory wrapperFactory,	PersonResidualMonthRecap mesePrecedente,
			Contract contract, int anno, int mese, int initMonteOreAnnoPassato,
			int initMonteOreAnnoCorrente, int initMealTickets,
			DateInterval validDataForPersonDay, 
			DateInterval validDataForCompensatoryRest, 
			DateInterval validDataForMealTickets,
			ConfYearDao confYearDao,CompetenceCodeDao competenceCodeDao,
			WorkingTimeTypeDayDao workingTimeTypeDayDao,
			DateUtility dateUtility) {

		this.dateUtility = dateUtility;
		this.confYearDao = confYearDao;
		this.competenceCodeDao = competenceCodeDao;
		this.workingTimeTypeDayDao = workingTimeTypeDayDao;
		this.absenceDao = absenceDao;
		this.personDayDao = personDayDao;
		this.mealTicketDao = mealTicketDao;
		this.competenceDao = competenceDao;
		this.wrapperFactory = wrapperFactory;
		this.mesePrecedente = mesePrecedente;
		this.contract = this.wrapperFactory.create(contract);
		this.person = contract.person;
		this.qualifica = this.person.qualification.qualification;
		this.anno = anno;
		this.mese = mese;

		this.initMonteOreAnnoCorrente = initMonteOreAnnoCorrente;
		this.initMonteOreAnnoPassato = initMonteOreAnnoPassato;


		//Per stampare a video il residuo da inizializzazione se riferito al mese
		if(contract.sourceDate != null && 
				contract.sourceDate.getMonthOfYear() == mese && 
				contract.sourceDate.getYear() == anno) {
			initResiduoAnnoCorrenteNelMese = contract.sourceRemainingMinutesCurrentYear;
		}

		setContractDescription(this);

		//Inizializzazione residui
		//Gennaio
		ConfYear confYear = null;
		Optional<ConfYear> conf = null;
		String description = qualifica > 3 ? 
				Parameter.MONTH_EXPIRY_RECOVERY_DAYS_49.description : 
					Parameter.MONTH_EXPIRY_RECOVERY_DAYS_13.description;
		conf = confYearDao.getByFieldName(description, anno, person.office);

		if(conf.isPresent()){
			confYear = conf.get();
		}
		else{
			confYear = confYearDao.getByFieldName(
					description, anno-1, person.office).get();
		}
		if(mese==1)
		{
			mesePrecedente = null;
			monteOreAnnoPassato = initMonteOreAnnoPassato;
			monteOreAnnoCorrente = initMonteOreAnnoCorrente;

			//se il residuo iniziale e' negativo lo tolgo dal residio mensile positivo
			if(monteOreAnnoPassato<0)
			{
				progressivoFinalePositivoMese = progressivoFinalePositivoMese + monteOreAnnoPassato;
				monteOreAnnoPassato = 0;
			}
		}

		//Febbraio / Marzo
		//		else if(mese==2 || mese==3)
		//		{
		//			this.mesePrecedente = mesePrecedente;
		//			monteOreAnnoPassato = initMonteOreAnnoPassato;
		//			monteOreAnnoCorrente= initMonteOreAnnoCorrente;
		//		}

		// Aprile -> Dicembre
		else
		{
			this.mesePrecedente = mesePrecedente;
			monteOreAnnoPassato = initMonteOreAnnoPassato;
			monteOreAnnoCorrente= initMonteOreAnnoCorrente;

			if(new Integer(confYear.fieldValue) != 0 && mese > new Integer(confYear.fieldValue))
			{
				possibileUtilizzareResiduoAnnoPrecedente = false;
				monteOreAnnoPassato = 0;
			}
		}

		//Inizializzazione buoni pasto
		if(mese==1) 
		{
			mesePrecedente = null;
			buoniPastoDalMesePrecedente = initMealTickets;
		}
		else if(mesePrecedente != null)
		{
			buoniPastoDalMesePrecedente = 
					mesePrecedente.buoniPastoDalMesePrecedente 
					+ mesePrecedente.buoniPastoConsegnatiNelMese
					- mesePrecedente.buoniPastoUsatiNelMese;
		}

		setMealTicketsInformation(this, validDataForMealTickets);

		setPersonDayInformation(this, validDataForPersonDay);
		setPersonMonthInformation(this, validDataForCompensatoryRest);


		assegnaProgressivoFinaleNegativo(this);
		assegnaStraordinari(this);
		assegnaRiposiCompensativi(this);

		//All'anno corrente imputo sia ciò che ho imputato al residuo del mese precedente dell'anno corrente sia ciò che ho imputato al progressivo finale positivo del mese
		//perchè non ho interesse a visualizzarli separati nel template. 
		progressivoFinaleNegativoMeseImputatoAnnoCorrente = progressivoFinaleNegativoMeseImputatoAnnoCorrente + progressivoFinaleNegativoMeseImputatoProgressivoFinalePositivoMese;
		riposiCompensativiMinutiImputatoAnnoCorrente = riposiCompensativiMinutiImputatoAnnoCorrente + riposiCompensativiMinutiImputatoProgressivoFinalePositivoMese;

		//Al monte ore dell'anno corrente aggiungo ciò che non ho utilizzato del progressivo finale positivo del mese
		monteOreAnnoCorrente = monteOreAnnoCorrente + progressivoFinalePositivoMese;	
	}

	/**
	 * 
	 * @param validDataForPersonDay l'intervallo all'interno del quale ricercare i person day per il calcolo dei progressivi
	 */
	private void setPersonDayInformation(PersonResidualMonthRecap monthRecap, DateInterval validDataForPersonDay)
	{
		if(validDataForPersonDay!=null)
		{
			List<PersonDay> pdList = personDayDao.getPersonDayInPeriodDesc(monthRecap.person,
					validDataForPersonDay.getBegin(), validDataForPersonDay.getEnd(), true);

			//progressivo finale fine mese
			for(PersonDay pd : pdList){
				if(pd != null){
					monthRecap.progressivoFinaleMese = pd.progressive;
					break;
				}
				else{
					//
				}
			}

			//progressivo finale positivo e negativo mese
			for(PersonDay pd : pdList)
			{
				if(pd.difference>=0)
					monthRecap.progressivoFinalePositivoMese += pd.difference;
				else
					monthRecap.progressivoFinaleNegativoMese += pd.difference;

				monthRecap.oreLavorate += pd.timeAtWork;
			}
			monthRecap.progressivoFinaleNegativoMese = monthRecap.progressivoFinaleNegativoMese*-1;

			monthRecap.progressivoFinalePositivoMesePrint = monthRecap.progressivoFinalePositivoMese;

		}
	}

	/**
	 * 
	 * @param validDataForPersonDay l'intervallo all'interno del quale ricercare i person day per il calcolo dei progressivi
	 */
	private void setMealTicketsInformation(PersonResidualMonthRecap monthRecap, DateInterval validDataForMealTickets)
	{

		if(validDataForMealTickets!=null)
		{
			List<PersonDay> pdList = personDayDao.getPersonDayInPeriod(monthRecap.person,
					validDataForMealTickets.getBegin(), Optional.fromNullable(validDataForMealTickets.getEnd()), true);

			//buoni pasto utilizzati
			for(PersonDay pd : pdList){
				if(pd != null && pd.isTicketAvailable){
					monthRecap.buoniPastoUsatiNelMese++;
				}
			}

			//Numero ticket consegnati nel mese
			monthRecap.buoniPastoConsegnatiNelMese = 
					mealTicketDao.getMealTicketAssignedToPersonIntoInterval(
							monthRecap.contract.getValue(), validDataForMealTickets).size();

			//residuo
			monthRecap.buoniPastoResidui = monthRecap.buoniPastoDalMesePrecedente 
					+ monthRecap.buoniPastoConsegnatiNelMese - monthRecap.buoniPastoUsatiNelMese;

		}
	}

	/**
	 * 
	 * @param validDataForCompensatoryRest, l'intervallo all'interno del quale ricercare i riposi compensativi
	 */
	private void setPersonMonthInformation(PersonResidualMonthRecap monthRecap, DateInterval validDataForCompensatoryRest)
	{
		CompetenceCode s1 = competenceCodeDao.getCompetenceCodeByCode("S1");
		CompetenceCode s2 = competenceCodeDao.getCompetenceCodeByCode("S2");
		CompetenceCode s3 = competenceCodeDao.getCompetenceCodeByCode("S3");

		if(this.contract.isLastInMonth(monthRecap.mese, monthRecap.anno))	//gli straordinari li assegno solo all'ultimo contratto attivo del mese
		{
			//straordinari s1
			Optional<Competence> competenceS1 = competenceDao.getCompetence(monthRecap.person, monthRecap.anno, monthRecap.mese, s1);

			if(competenceS1.isPresent())
				monthRecap.straordinariMinutiS1Print = monthRecap.straordinariMinutiS1Print + (competenceS1.get().valueApproved * 60);
			else
				monthRecap.straordinariMinutiS1Print = 0;
			//straordinari s2
			Optional<Competence> competenceS2 = competenceDao.getCompetence(monthRecap.person, monthRecap.anno, monthRecap.mese, s2);


			if(competenceS2.isPresent())
				monthRecap.straordinariMinutiS2Print = monthRecap.straordinariMinutiS2Print + (competenceS2.get().valueApproved * 60);
			else
				monthRecap.straordinariMinutiS2Print = 0;
			//straordinari s3
			Optional<Competence> competenceS3 = competenceDao.getCompetence(monthRecap.person, monthRecap.anno, monthRecap.mese, s3);
			if(competenceS3.isPresent())
				monthRecap.straordinariMinutiS3Print = monthRecap.straordinariMinutiS3Print + (competenceS3.get().valueApproved * 60);
			else
				monthRecap.straordinariMinutiS3Print = 0;


			monthRecap.straordinariMinuti = monthRecap.straordinariMinutiS1Print + monthRecap.straordinariMinutiS2Print + monthRecap.straordinariMinutiS3Print;
		}

		if(validDataForCompensatoryRest!=null)
		{
			List<Absence> riposiCompensativi = absenceDao.getAbsenceByCodeInPeriod(Optional.fromNullable(monthRecap.person), Optional.fromNullable("91"), 
					validDataForCompensatoryRest.getBegin(), validDataForCompensatoryRest.getEnd(), 
					Optional.<JustifiedTimeAtWork>absent(), false, false);
			monthRecap.riposiCompensativiMinuti = 0;
			monthRecap.numeroRiposiCompensativi = 0;
			for(Absence abs : riposiCompensativi){
				monthRecap.riposiCompensativiMinuti = monthRecap.riposiCompensativiMinuti + 
						workingTimeTypeDayDao.getWorkingTimeTypeDay(person, abs.personDay.date).workingTime;	//FIXME potrebbe essere null
				monthRecap.numeroRiposiCompensativi++;
			}
			monthRecap.riposiCompensativiMinutiPrint = monthRecap.riposiCompensativiMinuti;

		}		

	}

	private void assegnaProgressivoFinaleNegativo(PersonResidualMonthRecap monthRecap)
	{

		//quello che assegno al monte ore passato
		if(monthRecap.progressivoFinaleNegativoMese < monthRecap.monteOreAnnoPassato)
		{
			monthRecap.monteOreAnnoPassato = monthRecap.monteOreAnnoPassato - monthRecap.progressivoFinaleNegativoMese;
			monthRecap.progressivoFinaleNegativoMeseImputatoAnnoPassato = monthRecap.progressivoFinaleNegativoMese;
			return;
		}
		else
		{
			monthRecap.progressivoFinaleNegativoMeseImputatoAnnoPassato = monthRecap.monteOreAnnoPassato;
			monthRecap.monteOreAnnoPassato = 0;
			monthRecap.progressivoFinaleNegativoMese = monthRecap.progressivoFinaleNegativoMese - monthRecap.progressivoFinaleNegativoMeseImputatoAnnoPassato;
		}

		//quello che assegno al monte ore corrente
		if(monthRecap.progressivoFinaleNegativoMese < monthRecap.monteOreAnnoCorrente)
		{
			monthRecap.monteOreAnnoCorrente = monthRecap.monteOreAnnoCorrente - monthRecap.progressivoFinaleNegativoMese;
			monthRecap.progressivoFinaleNegativoMeseImputatoAnnoCorrente = monthRecap.progressivoFinaleNegativoMese;
			return;
		}
		else
		{
			monthRecap.progressivoFinaleNegativoMeseImputatoAnnoCorrente = monthRecap.monteOreAnnoCorrente;
			monthRecap.monteOreAnnoCorrente = 0;
			monthRecap.progressivoFinaleNegativoMese = monthRecap.progressivoFinaleNegativoMese - monthRecap.progressivoFinaleNegativoMeseImputatoAnnoCorrente;
		}

		//quello che assegno al progressivo positivo del mese
		monthRecap.progressivoFinalePositivoMese = monthRecap.progressivoFinalePositivoMese - monthRecap.progressivoFinaleNegativoMese;
		monthRecap.progressivoFinaleNegativoMeseImputatoProgressivoFinalePositivoMese = monthRecap.progressivoFinaleNegativoMese;
		return;

	}

	private void assegnaStraordinari(PersonResidualMonthRecap monthRecap)
	{
		monthRecap.progressivoFinalePositivoMese = monthRecap.progressivoFinalePositivoMese - monthRecap.straordinariMinuti;
	}

	private void assegnaRiposiCompensativi(PersonResidualMonthRecap monthRecap)
	{
		//quello che assegno al monte ore passato
		if(monthRecap.riposiCompensativiMinuti < monthRecap.monteOreAnnoPassato)
		{
			monthRecap.monteOreAnnoPassato = monthRecap.monteOreAnnoPassato - monthRecap.riposiCompensativiMinuti;
			monthRecap.riposiCompensativiMinutiImputatoAnnoPassato = monthRecap.riposiCompensativiMinuti;
			return;
		}
		else
		{
			monthRecap.riposiCompensativiMinutiImputatoAnnoPassato = monthRecap.monteOreAnnoPassato;
			monthRecap.monteOreAnnoPassato = 0;
			monthRecap.riposiCompensativiMinuti = monthRecap.riposiCompensativiMinuti - monthRecap.riposiCompensativiMinutiImputatoAnnoPassato;
		}

		//quello che assegno al monte ore corrente
		if(monthRecap.riposiCompensativiMinuti < monthRecap.monteOreAnnoCorrente)
		{
			monthRecap.monteOreAnnoCorrente = monthRecap.monteOreAnnoCorrente - monthRecap.riposiCompensativiMinuti;
			monthRecap.riposiCompensativiMinutiImputatoAnnoCorrente = monthRecap.riposiCompensativiMinuti;
			return;
		}
		else
		{
			monthRecap.riposiCompensativiMinutiImputatoAnnoCorrente = monthRecap.monteOreAnnoCorrente;
			monthRecap.monteOreAnnoCorrente = 0;
			monthRecap.riposiCompensativiMinuti = monthRecap.riposiCompensativiMinuti - monthRecap.riposiCompensativiMinutiImputatoAnnoCorrente;
		}
		//quello che assegno al progressivo positivo del mese
		monthRecap.progressivoFinalePositivoMese = monthRecap.progressivoFinalePositivoMese - monthRecap.riposiCompensativiMinuti;
		monthRecap.riposiCompensativiMinutiImputatoProgressivoFinalePositivoMese = monthRecap.riposiCompensativiMinuti;

	}	

	/**
	 * Costruisce una stringa di descrizione per il contratto utilizzata in stampings.html e personStampings.html
	 */
	private void setContractDescription(PersonResidualMonthRecap monthRecap)
	{
		LocalDate beginMonth = new LocalDate(monthRecap.anno, monthRecap.mese, 1);
		LocalDate endMonth = beginMonth.dayOfMonth().withMaximumValue();
		DateInterval monthInterval = new DateInterval(beginMonth, endMonth);	
		LocalDate endContract = monthRecap.contract.getValue().expireContract;
		if(monthRecap.contract.getValue().endContract!=null)
			endContract = monthRecap.contract.getValue().endContract;

		if(dateUtility.isDateIntoInterval(endContract, monthInterval))
			monthRecap.contractDescription = "(contratto scaduto in data " + endContract+")";
		else
			monthRecap.contractDescription = "";
	}
}


