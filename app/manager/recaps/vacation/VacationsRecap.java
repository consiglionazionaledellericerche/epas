package manager.recaps.vacation;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.ArrayList;
import java.util.List;

import manager.ConfYearManager;
import manager.VacationManager;
import models.Absence;
import models.AbsenceType;
import models.Contract;
import models.ContractMonthRecap;
import models.Person;
import models.VacationPeriod;
import models.enumerate.AbsenceTypeMapping;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import dao.AbsenceDao;
import dao.AbsenceTypeDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;


/**
 * @author alessandro
 * Classe da utilizzare per il riepilogo delle informazioni relative al piano ferie di una persona.
 */
public class VacationsRecap {

	public Person person;
	public int year;
	public Contract contract = null;
	public DateInterval activeContractInterval = null;

	public List<Absence> vacationDaysLastYearUsed = new ArrayList<Absence>();
	public List<Absence> vacationDaysCurrentYearUsed = new ArrayList<Absence>();
	public List<Absence> permissionUsed = new ArrayList<Absence>();

	public Integer vacationDaysLastYearAccrued = 0;
	public Integer vacationDaysCurrentYearAccrued = 0;
	public Integer permissionCurrentYearAccrued = 0;

	public Integer vacationDaysLastYearNotYetUsed = 0;
	public Integer vacationDaysCurrentYearNotYetUsed = 0;
	public Integer persmissionNotYetUsed = 0;

	public Integer vacationDaysCurrentYearTotal = 0;
	public Integer permissionCurrentYearTotal = 0;

	public boolean isExpireLastYear = false;		/* true se le ferie dell'anno passato sono scadute */

	public boolean isExpireBeforeEndYear = false;	/* true se il contratto termina prima della fine dell'anno richiesto */
	public boolean isActiveAfterBeginYear = false;	/* true se il contratto inizia dopo l'inizio dell'anno richiesto */

	//private final static Logger log = LoggerFactory.getLogger(ContractManager.class);


	private final AbsenceDao absenceDao;
	private final AbsenceTypeDao absenceTypeDao;
	private final IWrapperFactory wrapperFactory;

	/**
	 * @param person
	 * @param year l'anno in considerazione
	 * @param contract il contratto di cui si vuole calcolare il riepilogo.
	 * @param actualDate la data specifica dell'anno attuale in cui si desidera fotografare 
	 * la situazione in termini di ferie e permessi maturati (tipicamente oggi)
	 * @param considerExpireLastYear impostare false se non si vuole considerare il limite di scadenza per l'utilizzo
	 * @param contractManager 
	 * @return
	 */
	public VacationsRecap(IWrapperFactory wrapperFactory, AbsenceDao absenceDao,
			AbsenceTypeDao absenceTypeDao, ConfYearManager confYearManager, 
			VacationManager vacationManager,
			int year, Contract contract, LocalDate actualDate, 
			boolean considerExpireLastYear) {

		this.absenceDao = absenceDao;
		this.absenceTypeDao = absenceTypeDao;
		this.wrapperFactory = wrapperFactory;

		this.person = contract.person;
		this.year = year;
		this.contract = contract;

		IWrapperContract c = wrapperFactory.create(contract);
		
		this.activeContractInterval = c.getContractDateInterval();

		LocalDate today = LocalDate.now();
		LocalDate startLastYear = new LocalDate(this.year-1,1,1);	
		LocalDate endLastYear = new LocalDate(this.year-1,12,31);	
		LocalDate startYear = new LocalDate(this.year,1,1);
		LocalDate endYear = new LocalDate(this.year,12,31);
		DateInterval lastYearInter = new DateInterval(startLastYear, endLastYear);
		DateInterval yearInter = new DateInterval(startYear, endYear);
		DateInterval yearActualDateInter = new DateInterval(startYear, actualDate);

		AbsenceType ab32  = absenceTypeDao.getAbsenceTypeByCode(AbsenceTypeMapping.FERIE_ANNO_CORRENTE.getCode()).orNull();
		AbsenceType ab31  = absenceTypeDao.getAbsenceTypeByCode(AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE.getCode()).orNull();
		AbsenceType ab37  = absenceTypeDao.getAbsenceTypeByCode(AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE_DOPO_31_08.getCode()).orNull();
		AbsenceType ab94  = absenceTypeDao.getAbsenceTypeByCode(AbsenceTypeMapping.FESTIVITA_SOPPRESSE.getCode()).orNull();

		//Expire Last Year
		LocalDate expireVacation = vacationManager.vacationsLastYearExpireDate(year, this.person.office);

		this.isExpireLastYear = false;

		if( this.year < today.getYear() ) {		//query anni passati 
			this.isExpireLastYear = true;
		}
		else if( this.year == today.getYear() && actualDate.isAfter(expireVacation)) {	//query anno attuale
			this.isExpireLastYear = true;
		}

		//Expire Before End Of Year / Active After Begin Of Year

		if(this.activeContractInterval.getEnd().isBefore(endYear))
			this.isExpireBeforeEndYear = true;
		if(this.activeContractInterval.getBegin().isAfter(startYear))
			this.isActiveAfterBeginYear = true;
		
		////////////////////////////////////////////////////////////////////////
		// (1) Calcolo ferie usate dell'anno passato 
		////////////////////////////////////////////////////////////////////////
		List<Absence> abs32Last = null;
		List<Absence> abs31Last = null;
		List<Absence> abs37Last = null;

		int vacationDaysPastYearUsedNew = 0;
		
		//Popolare da source data 
		if (this.contract.sourceDate != null 
				&& this.contract.sourceDate.getYear() == year) {
			vacationDaysPastYearUsedNew += this.contract.sourceVacationLastYearUsed;
			DateInterval yearInterSource = new DateInterval(this.contract.sourceDate.plusDays(1), endYear);
			abs31Last = absenceDao.getAbsenceDays(yearInterSource, this.contract, ab31);										
			abs37Last = absenceDao.getAbsenceDays(yearInterSource, this.contract, ab37);
			vacationDaysPastYearUsedNew += abs31Last.size() + abs37Last.size();
		}
		
		//Non esiste anno passato nel presente contratto
		else if (this.contract.beginContract.getYear() == this.year) {
			vacationDaysPastYearUsedNew = 0;
			abs31Last  = new ArrayList<Absence>();
			abs37Last  = new ArrayList<Absence>();
		}
		
		//Caso in cui voglio inserire ferie per l'anno prossimo
		else if(this.year > LocalDate.now().getYear()) {
			VacationsRecap vrPastYear = new VacationsRecap(wrapperFactory, 
					absenceDao, absenceTypeDao, confYearManager,
					vacationManager,
					this.year - 1, this.contract, endLastYear, true);
			
			abs31Last = absenceDao.getAbsenceDays(yearInter, this.contract, ab31);						
			abs37Last = absenceDao.getAbsenceDays(yearInter, this.contract, ab37);	
			vacationDaysPastYearUsedNew = vrPastYear.vacationDaysCurrentYearUsed.size() 
					+ abs31Last.size() + abs37Last.size();
		
		//Caso generale: popolare da riepilogo fine anno 
		} else {
			
			//(TODO: l'esistenza dovrebbe essere garantita dal factory, verificare)
			Optional<ContractMonthRecap> recapPastYear = c
					.getContractMonthRecap( new YearMonth(year-1,12) );
			
			Preconditions.checkState(recapPastYear.isPresent());

			vacationDaysPastYearUsedNew = recapPastYear.get().vacationCurrentYearUsed;
			abs31Last = absenceDao.getAbsenceDays(yearInter, this.contract, ab31);						
			abs37Last = absenceDao.getAbsenceDays(yearInter, this.contract, ab37);						
			vacationDaysPastYearUsedNew += abs31Last.size() + abs37Last.size();
		}
		//costruisco la lista delle ferie per stampare le date 
		//(prendo tutto ciò che trovo nel db e poi riempo con null fino alla dimensione calcolata)
		abs32Last  = absenceDao.getAbsenceDays(lastYearInter, this.contract, ab32);

		this.vacationDaysLastYearUsed.addAll(abs32Last);
		this.vacationDaysLastYearUsed.addAll(abs31Last);
		this.vacationDaysLastYearUsed.addAll(abs37Last);
		while (this.vacationDaysLastYearUsed.size() < vacationDaysPastYearUsedNew)
		{
			Absence nullAbsence = null;
			this.vacationDaysLastYearUsed.add(nullAbsence);
		}

		////////////////////////////////////////////////////////////////////////
		// (2) Calcolo ferie usate dell'anno corrente 
		////////////////////////////////////////////////////////////////////////
		List<Absence> abs32Current  = null;

		int vacationDaysCurrentYearUsedNew = 0;
		if(this.contract.sourceDate!=null && this.contract.sourceDate.getYear()==year)
		{
			vacationDaysCurrentYearUsedNew += this.contract.sourceVacationCurrentYearUsed;
			DateInterval yearInterSource = new DateInterval(this.contract.sourceDate.plusDays(1), endYear);
			abs32Current = absenceDao.getAbsenceDays(yearInterSource, this.contract, ab32);
			vacationDaysCurrentYearUsedNew += abs32Current.size();
		}
		else
		{
			abs32Current = absenceDao.getAbsenceDays(yearInter, this.contract, ab32);
			vacationDaysCurrentYearUsedNew += abs32Current.size();
		}
		this.vacationDaysCurrentYearUsed.addAll(abs32Current);
		while(this.vacationDaysCurrentYearUsed.size() < vacationDaysCurrentYearUsedNew)
		{
			Absence nullAbsence = null;
			this.vacationDaysCurrentYearUsed.add(nullAbsence);
		}

		////////////////////////////////////////////////////////////////////////
		// //(3) Calcolo permessi usati dell'anno corrente 
		////////////////////////////////////////////////////////////////////////
		List<Absence> abs94Current = null;
		int permissionCurrentYearUsedNew = 0;

		if(this.contract.sourceDate!=null && this.contract.sourceDate.getYear()==year)
		{
			permissionCurrentYearUsedNew += this.contract.sourcePermissionUsed;
			DateInterval yearInterSource = new DateInterval(this.contract.sourceDate.plusDays(1), endYear);
			abs94Current = absenceDao.getAbsenceDays(yearInterSource, this.contract, ab94);
			permissionCurrentYearUsedNew += abs94Current.size();
		}
		else
		{
			abs94Current = absenceDao.getAbsenceDays(yearInter, this.contract, ab94);
			permissionCurrentYearUsedNew += abs94Current.size();
		}
		this.permissionUsed.addAll(abs94Current);
		while(this.permissionUsed.size()<permissionCurrentYearUsedNew)
		{
			Absence nullAbsence = null;
			this.permissionUsed.add(nullAbsence);
		}


		//(4) Calcolo ferie e permessi maturati per l'anno passato e l'anno corrente (sono indipendenti dal database)
		this.vacationDaysLastYearAccrued = getVacationAccruedYear(lastYearInter, contract);
		if(endYear.isAfter(actualDate))
		{
			//se la query e' per l'anno corrente considero fino a actualDate
			this.permissionCurrentYearAccrued = getPermissionAccruedYear( yearActualDateInter, contract);
			this.vacationDaysCurrentYearAccrued = getVacationAccruedYear( yearActualDateInter, contract);
		}
		else
		{
			//se la query e' per gli anni passati considero fino a endYear
			this.permissionCurrentYearAccrued = getPermissionAccruedYear(yearInter, contract);
			this.vacationDaysCurrentYearAccrued = getVacationAccruedYear(yearInter, contract);
		}

		//(5)Calcolo ferie e permessi non ancora utilizzati per l'anno corrente e per l'anno precedente (sono funzione di quanto calcolato precedentemente)
		//Anno passato
		if(actualDate.isBefore(expireVacation) || !considerExpireLastYear){
			this.vacationDaysLastYearNotYetUsed = this.vacationDaysLastYearAccrued - this.vacationDaysLastYearUsed.size();
			if(this.vacationDaysLastYearAccrued == 25)
				this.vacationDaysLastYearNotYetUsed++; 
		}
		else
			this.vacationDaysLastYearNotYetUsed = 0;
		//Anno corrente
		this.permissionCurrentYearTotal = getPermissionAccruedYear(yearInter, contract);
		this.vacationDaysCurrentYearTotal = getVacationAccruedYear(yearInter, contract);
		if(this.contract.expireContract != null) {
			//per i detereminati considero le maturate (perchè potrebbero decidere di cambiare contratto)
			this.vacationDaysCurrentYearNotYetUsed = this.vacationDaysCurrentYearAccrued - this.vacationDaysCurrentYearUsed.size();
			this.persmissionNotYetUsed = this.permissionCurrentYearAccrued - this.permissionUsed.size();
		}
		else 
		{
			//per gli indeterminati le considero tutte (è più improbabile....)
			this.vacationDaysCurrentYearNotYetUsed = this.vacationDaysCurrentYearTotal - this.vacationDaysCurrentYearUsed.size();
			this.persmissionNotYetUsed = this.permissionCurrentYearTotal - this.permissionUsed.size();
		}

	}

	/**
	 * 
	 * @param yearInterval
	 * @param contract
	 * @return numero di permessi maturati nel periodo yearInterval associati a contract
	 */
	private int getPermissionAccruedYear(DateInterval yearInterval, Contract contract){
		//int days = 0;
		int permissionDays = 0;

		//Calcolo l'intersezione fra l'anno e il contratto attuale
		yearInterval = DateUtility.intervalIntersection(yearInterval, 
				new DateInterval(contract.beginContract, contract.expireContract));
		if(yearInterval == null) {
			return 0;
		}


		for(VacationPeriod vp : contract.vacationPeriods){
			int days = 0;
			DateInterval vpInterval = new DateInterval(vp.beginFrom, vp.endTo);
			DateInterval intersection = 
					DateUtility.intervalIntersection(vpInterval, yearInterval);
			
			if(intersection != null) {
				days = DateUtility.daysInInterval(intersection);
			}
			if(vp.vacationCode.equals("21+3")){
				permissionDays = permissionDays + VacationsPermissionsDaysAccrued
						.convertWorkDaysToPermissionDaysPartTime(days);
				
			} else {
				permissionDays = permissionDays + VacationsPermissionsDaysAccrued
						.convertWorkDaysToPermissionDays(days);
			}
		}

		return permissionDays;
	}

	/**
	 * 
	 * @param yearInterval
	 * @param contract
	 * @param vacationPeriodList
	 * @return il numero di giorni di ferie maturati nell'anno year 
	 * 	calcolati a partire dai piani ferie associati al contratto corrente
	 */
	private int getVacationAccruedYear(DateInterval yearInterval, Contract contract){

		int vacationDays = 0;

		//Calcolo l'intersezione fra l'anno e il contratto attuale
		yearInterval = DateUtility.intervalIntersection(yearInterval, 
				new DateInterval(contract.beginContract, contract.expireContract));
		if(yearInterval == null) {
			return 0;
		}

		//per ogni piano ferie conto i giorni trascorsi in yearInterval e applico la funzione di conversione
		for(VacationPeriod vp : contract.vacationPeriods) {
			
			int days = 0;
			DateInterval vpInterval = new DateInterval(vp.beginFrom, vp.endTo);
			DateInterval intersection = DateUtility.intervalIntersection(vpInterval, yearInterval);
			if(intersection != null) {
				days = DateUtility.daysInInterval(intersection);
			}

			//calcolo i giorni maturati col metodo di conversione
			List<Absence> absences = accruedVacationDays(intersection, contract);
			if(vp.vacationCode.description.equals("26+4")) {
				vacationDays = vacationDays + VacationsPermissionsDaysAccrued
						.convertWorkDaysToVacationDaysLessThreeYears(days-absences.size());
			}
			if(vp.vacationCode.description.equals("28+4")) {
				vacationDays = vacationDays + VacationsPermissionsDaysAccrued
						.convertWorkDaysToVacationDaysMoreThreeYears(days-absences.size());
			}
			if(vp.vacationCode.description.equals("21+3")){
				vacationDays = vacationDays + VacationsPermissionsDaysAccrued
						.converWorkDaysToVacationDaysPartTime(days-absences.size());
			}

		}

		//FIXME decidere se deve essere un parametro di configurazione
		if(vacationDays>28) {
			vacationDays = 28;
		}

		return vacationDays;

	}

	/**
	 * 
	 * @param intersection
	 * @param contract
	 * @return la lista dei giorni di assenza in cui si è usato un codice di assenza per assistenza post partum
	 */
	private List<Absence> accruedVacationDays(DateInterval intersection, Contract contract){

		List<AbsenceType> postPartumCodeList = absenceTypeDao.getReducingAccruingDaysForVacations();

		DateInterval contractInterInterval = DateUtility.intervalIntersection(intersection, wrapperFactory.create(contract).getContractDateInterval());
		if(contractInterInterval==null)
			return new ArrayList<Absence>();

		List<Absence> absences = absenceDao.getAbsenceWithPostPartumCode(contract.person, contractInterInterval.getBegin(),
				contractInterInterval.getEnd(), postPartumCodeList, true);

		return absences;
	}

}
