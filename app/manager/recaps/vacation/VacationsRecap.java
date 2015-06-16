package manager.recaps.vacation;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.ArrayList;
import java.util.List;

import manager.ConfYearManager;
import manager.VacationManager;
import manager.cache.AbsenceTypeManager;
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
import com.google.common.collect.Lists;

import controllers.Absences;
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
	public IWrapperContract wcontract = null;
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

	private final AbsenceTypeManager absenceTypeManager;

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
			AbsenceTypeDao absenceTypeDao, AbsenceTypeManager absenceTypeManager,  
			ConfYearManager confYearManager, 
			VacationManager vacationManager,
			int year, Contract contract, Optional<LocalDate> accruedDate, 
			boolean considerExpireLastYear) {

		if(accruedDate.isPresent()) {
			Preconditions.checkArgument(year == accruedDate.get().getYear());
		} else {
			accruedDate = Optional.fromNullable(LocalDate.now());
		}
		
		this.absenceTypeManager = absenceTypeManager;

		this.person = contract.person;
		this.year = year;
		LocalDate startRequestYear = new LocalDate(this.year, 1, 1);
		LocalDate endRequestYear = new LocalDate(this.year, 12, 31);
		
		this.contract = contract;
		this.wcontract = wrapperFactory.create(contract);
		this.activeContractInterval = wcontract.getContractDateInterval();

		//Vacation Last Year Expired  
		LocalDate expireVacation = vacationManager
				.vacationsLastYearExpireDate(year, this.person.office);
		this.isExpireLastYear = false;
		if( this.year < LocalDate.now().getYear() ) {		
			this.isExpireLastYear = true;
		} else if( this.year == LocalDate.now().getYear() 
				&& accruedDate.get().isAfter(expireVacation)) {	
			this.isExpireLastYear = true;
		}

		//Contract Expire Before End Of Year / Active After Begin Of Year
		if(this.activeContractInterval.getEnd().isBefore(endRequestYear)) {
			this.isExpireBeforeEndYear = true;
		}
		if(this.activeContractInterval.getBegin().isAfter(startRequestYear)) {
			this.isActiveAfterBeginYear = true;
		}

		// Gli intervalli su cui predere le assenze nel db
		DateInterval previousYearInterval = DateUtility
				.intervalIntersection(wcontract.getContractDatabaseInterval(),
						new DateInterval(new LocalDate(this.year-1,1,1), 
								new LocalDate(this.year-1,12,31)));
		DateInterval requestYearInterval = DateUtility
				.intervalIntersection(wcontract.getContractDatabaseInterval(),
						new DateInterval(new LocalDate(this.year,1,1), 
								new LocalDate(this.year,12,31)));
		DateInterval nextYearInterval = DateUtility
				.intervalIntersection(wcontract.getContractDatabaseInterval(), 
						new DateInterval(new LocalDate(this.year+1,1,1), 
								new LocalDate(this.year+1,12,31)));

		// Le assenze
		List<Absence> absencesForVacationsRecap = absenceDao
				.getAbsencesInCodeList(person, previousYearInterval.getBegin(),
						nextYearInterval.getEnd(), absenceTypeManager.codesForVacations(), true);

		AbsenceType ab32  = absenceTypeManager.getAbsenceType(AbsenceTypeMapping.FERIE_ANNO_CORRENTE.getCode());
		AbsenceType ab31  = absenceTypeManager.getAbsenceType(AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE.getCode());
		AbsenceType ab37  = absenceTypeManager.getAbsenceType(AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE_DOPO_31_08.getCode());
		AbsenceType ab94  = absenceTypeManager.getAbsenceType(AbsenceTypeMapping.FESTIVITA_SOPPRESSE.getCode());

		//(1) ferie fatte dell'anno precedente all'anno richiesto
		List<Absence> vacationPreviousYearUsed = Lists.newArrayList();
		vacationPreviousYearUsed.addAll(filterAbsences(absencesForVacationsRecap, previousYearInterval, ab32));
		vacationPreviousYearUsed.addAll(filterAbsences(absencesForVacationsRecap, requestYearInterval, ab31));
		vacationPreviousYearUsed.addAll(filterAbsences(absencesForVacationsRecap, requestYearInterval, ab37));

		int vacationDaysPastYearUsedNew = vacationPreviousYearUsed.size();
		
		if (this.contract.sourceDate != null && this.contract.sourceDate.getYear() == year) {
			vacationDaysPastYearUsedNew += this.contract.sourceVacationLastYearUsed;
			
			while (this.vacationDaysLastYearUsed.size() < vacationDaysPastYearUsedNew) {
				this.vacationDaysLastYearUsed.add(null);
			}
		}
		
		//(2) ferie fatte dell'anno richiesto
		List<Absence> vacationRequestYearUsed = Lists.newArrayList();
		vacationRequestYearUsed.addAll(filterAbsences(absencesForVacationsRecap, requestYearInterval, ab32));
		vacationRequestYearUsed.addAll(filterAbsences(absencesForVacationsRecap, nextYearInterval, ab31));
		vacationRequestYearUsed.addAll(filterAbsences(absencesForVacationsRecap, nextYearInterval, ab37));
		
		int vacationDaysCurrentYearUsedNew = vacationRequestYearUsed.size();
		
		if(this.contract.sourceDate!=null && this.contract.sourceDate.getYear()==year) {
			vacationDaysCurrentYearUsedNew += this.contract.sourceVacationCurrentYearUsed;

			while (this.vacationDaysLastYearUsed.size() < vacationDaysCurrentYearUsedNew) {
				this.vacationDaysLastYearUsed.add(null);
			}
		}
		
		//(3) permessi usati dell'anno richiesto
		List<Absence> permissionRequestYearUsed = Lists.newArrayList();
		permissionRequestYearUsed.addAll(filterAbsences(absencesForVacationsRecap, requestYearInterval, ab94));
		
		int permissionCurrentYearUsedNew = permissionRequestYearUsed.size();
		
		if(this.contract.sourceDate!=null && this.contract.sourceDate.getYear() == year) {
			permissionCurrentYearUsedNew += this.contract.sourcePermissionUsed;
			while(this.permissionUsed.size() < permissionCurrentYearUsedNew) {
				this.permissionUsed.add(null);
			}
		}

		//(4) Calcolo ferie e permessi maturati per l'anno passato e l'anno corrente 
		// (sono indipendenti dal database)
		
		this.vacationDaysLastYearAccrued = 
				getVacationAccruedYear(year-1, Optional.<LocalDate>absent(), absencesForVacationsRecap);
		this.permissionCurrentYearAccrued = 
				getPermissionAccruedYear( year, accruedDate);
		this.vacationDaysCurrentYearAccrued = 
				getVacationAccruedYear( year, accruedDate, absencesForVacationsRecap);

		//(5) Calcolo ferie e permessi totali per l'anno corrente
		this.permissionCurrentYearTotal = 
				getPermissionAccruedYear(year, Optional.<LocalDate>absent());
		this.vacationDaysCurrentYearTotal = 
				getVacationAccruedYear(year, Optional.<LocalDate>absent(), absencesForVacationsRecap);
		
		//(6) Calcolo ferie e permessi non ancora utilizzati per l'anno corrente e per l'anno precedente 
		// (sono funzione di quanto calcolato precedentemente)
		
		//Anno passato
		if(accruedDate.get().isBefore(expireVacation) || !considerExpireLastYear){
			this.vacationDaysLastYearNotYetUsed = this.vacationDaysLastYearAccrued
					- this.vacationDaysLastYearUsed.size();
			if(this.vacationDaysLastYearAccrued == 25)
				this.vacationDaysLastYearNotYetUsed++; 
		}
		else {
			this.vacationDaysLastYearNotYetUsed = 0;
		}
		
		//Anno corrente
		if(this.wcontract.isDefined()) {
			//per i detereminati considero le maturate 
			//(perchè potrebbero decidere di cambiare contratto)
			this.vacationDaysCurrentYearNotYetUsed = this.vacationDaysCurrentYearAccrued
					- this.vacationDaysCurrentYearUsed.size();
			this.persmissionNotYetUsed = this.permissionCurrentYearAccrued
					- this.permissionUsed.size();
			
		} else {
			//per gli indeterminati le considero tutte (è più improbabile....)
			this.vacationDaysCurrentYearNotYetUsed = this.vacationDaysCurrentYearTotal
					- this.vacationDaysCurrentYearUsed.size();
			this.persmissionNotYetUsed = this.permissionCurrentYearTotal
					- this.permissionUsed.size();
		}

	}

	/**
	 * 
	 * @param yearInterval
	 * @param contract
	 * @return numero di permessi maturati nel periodo yearInterval associati a contract
	 */
	private int getPermissionAccruedYear(int year, Optional<LocalDate> accruedDate) {
		
		//Calcolo l'intersezione fra l'anno e il contratto attuale
		DateInterval yearInterval = new DateInterval(new LocalDate(year,1,1), 
				new LocalDate(year,12,31));
		if(accruedDate.isPresent()) {
			yearInterval = new DateInterval(new LocalDate(year,1,1), 
				accruedDate.get());
		}
		yearInterval = DateUtility.intervalIntersection(yearInterval, 
				this.wcontract.getContractDateInterval());
						
		if(yearInterval == null) {
			return 0;
		}
		
		//int days = 0;
		int permissionDays = 0;

		for(VacationPeriod vp : this.wcontract.getValue().vacationPeriods){
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
	private int getVacationAccruedYear(int year, Optional<LocalDate> accruedDate, 
			List<Absence> absencesForVacationsRecap){

		//Calcolo l'intersezione fra l'anno e il contratto attuale
		DateInterval yearInterval = new DateInterval(new LocalDate(year,1,1), 
				new LocalDate(year,12,31));
		if(accruedDate.isPresent()) {
			yearInterval = new DateInterval(new LocalDate(year,1,1), 
					accruedDate.get());
		}
		yearInterval = DateUtility.intervalIntersection(yearInterval, 
				this.wcontract.getContractDateInterval());

		if(yearInterval == null) {
			return 0;
		}

		int vacationDays = 0;
		//per ogni piano ferie conto i giorni trascorsi in yearInterval 
		// e applico la funzione di conversione
		for(VacationPeriod vp : this.wcontract.getValue().vacationPeriods) {
			
			int days = 0;
			
			DateInterval vpInterval = new DateInterval(vp.beginFrom, vp.endTo);
			DateInterval intersection = 
					DateUtility.intervalIntersection(vpInterval, yearInterval);
			
			if(intersection != null) {
				
				days = DateUtility.daysInInterval(intersection) 
						- postPartumAbsences(intersection, absencesForVacationsRecap);
				
				//calcolo i giorni maturati col metodo di conversione
				
				if(vp.vacationCode.description.equals("26+4")) {
					vacationDays = vacationDays + VacationsPermissionsDaysAccrued
							.convertWorkDaysToVacationDaysLessThreeYears(days);
				}
				if(vp.vacationCode.description.equals("28+4")) {
					vacationDays = vacationDays + VacationsPermissionsDaysAccrued
							.convertWorkDaysToVacationDaysMoreThreeYears(days);
				}
				if(vp.vacationCode.description.equals("21+3")){
					vacationDays = vacationDays + VacationsPermissionsDaysAccrued
							.converWorkDaysToVacationDaysPartTime(days);
				}
			}
		}

		//FIXME decidere se deve essere un parametro di configurazione
		if (vacationDays > 28) {
			vacationDays = 28;
		}

		return vacationDays;

	}

	/**
	 * 
	 * @param intersection
	 * @param contract
	 * @return la lista dei giorni di assenza in cui si è usato 
	 * un codice di assenza per assistenza post partum
	 */
	private int postPartumAbsences(DateInterval interval, List<Absence> absences){
		
		return filterAbsences(absences, interval, 
				absenceTypeManager.postPartumCodes()).size();

//		return absenceDao.getAbsencesInCodeList(
//				person, interval.getBegin(), interval.getEnd(), 
//				absenceTypeManager.postPartumCodes(), true).size();
	}
	
	private List<Absence> filterAbsences(List<Absence> absences, DateInterval interval, 
			List<AbsenceType> types) {
		
		List<Absence> abs = Lists.newArrayList();
		
		for(Absence ab : absences) {
			if(DateUtility.isDateIntoInterval(ab.personDay.date, interval)) {
				if(types.contains(ab.absenceType)) {
					abs.add(ab);
				}
			}
		}
		
		return abs;
	}
	
	private List<Absence> filterAbsences(List<Absence> absences, DateInterval interval, 
			AbsenceType type) {
		List<AbsenceType> types = Lists.newArrayList();
		types.add(type);
		return filterAbsences(absences, interval, types);
	}

	
}
