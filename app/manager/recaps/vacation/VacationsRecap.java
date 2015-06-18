package manager.recaps.vacation;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.List;

import manager.ConfYearManager;
import manager.VacationManager;
import manager.cache.AbsenceTypeManager;
import models.Absence;
import models.AbsenceType;
import models.Contract;
import models.Person;
import models.VacationPeriod;
import models.enumerate.AbsenceTypeMapping;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

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
	public LocalDate accruedDate;

	public int vacationDaysLastYearUsed = 0;
	public int vacationDaysCurrentYearUsed = 0;
	public int permissionUsed = 0;

	public Integer vacationDaysLastYearAccrued = 0;
	public Integer vacationDaysCurrentYearAccrued = 0;
	public Integer permissionCurrentYearAccrued = 0;

	public Integer vacationDaysLastYearNotYetUsed = 0;
	public Integer vacationDaysCurrentYearNotYetUsed = 0;
	public Integer persmissionNotYetUsed = 0;

	public Integer vacationDaysCurrentYearTotal = 0;
	public Integer permissionCurrentYearTotal = 0;

	public LocalDate dateExpireLastYear;
	public boolean isExpireLastYear = false;		/* true se le ferie dell'anno passato sono scadute */

	public boolean isExpireBeforeEndYear = false;	/* true se il contratto termina prima della fine dell'anno richiesto */
	public boolean isActiveAfterBeginYear = false;	/* true se il contratto inizia dopo l'inizio dell'anno richiesto */

	private final AbsenceTypeManager absenceTypeManager;
	
	private DateInterval previousYearInterval;
	private DateInterval requestYearInterval;
	private DateInterval nextYearInterval;
	
	private List<Absence> list32PreviouYear = Lists.newArrayList();
	private List<Absence> list31RequestYear = Lists.newArrayList();
	private List<Absence> list37RequestYear = Lists.newArrayList();
	
	private List<Absence> list32RequestYear = Lists.newArrayList();
	private List<Absence> list31NextYear = Lists.newArrayList();
	private List<Absence> list37NextYear = Lists.newArrayList();
	
	private List<Absence> list94RequestYear = Lists.newArrayList();

	private List<Absence> postPartum = Lists.newArrayList();
	
	private final AbsenceDao absenceDao;
	private final VacationManager vacationManager;

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

		this.absenceDao = absenceDao;
		this.vacationManager = vacationManager;
		this.absenceTypeManager = absenceTypeManager;
		
		if(accruedDate.isPresent()) {
			//Preconditions.checkArgument(year == accruedDate.get().getYear());
		} else {
			accruedDate = Optional.fromNullable(LocalDate.now());
		}
		this.accruedDate = accruedDate.get();

		this.person = contract.person;
		this.year = year;
		
		this.contract = contract;
		this.wcontract = wrapperFactory.create(contract);
		this.activeContractInterval = wcontract.getContractDateInterval();
		
		initDataStructures();

		//(1) ferie fatte dell'anno precedente all'anno richiesto
		vacationDaysLastYearUsed = list32PreviouYear.size() 
				+ list31RequestYear.size() + list37RequestYear.size();
		
		//(2) ferie fatte dell'anno richiesto
		vacationDaysCurrentYearUsed = list32RequestYear.size() 
				+ list31NextYear.size() + list37NextYear.size();
		
		//(3) permessi usati dell'anno richiesto
		permissionUsed = list94RequestYear.size();
		
		if (this.contract.sourceDate != null && this.contract.sourceDate.getYear() == year) {
			vacationDaysLastYearUsed += this.contract.sourceVacationLastYearUsed;
			vacationDaysCurrentYearUsed += this.contract.sourceVacationCurrentYearUsed;
			permissionUsed += this.contract.sourcePermissionUsed;
		}
		
		//(4) Calcolo ferie e permessi maturati per l'anno passato e l'anno corrente 
		// (sono indipendenti dal database)
		
		this.vacationDaysLastYearAccrued = 
				getVacationAccruedYear(year-1, Optional.<LocalDate>absent());
		this.permissionCurrentYearAccrued = 
				getPermissionAccruedYear( year, accruedDate);
		this.vacationDaysCurrentYearAccrued = 
				getVacationAccruedYear( year, accruedDate);

		//(5) Calcolo ferie e permessi totali per l'anno corrente
		this.permissionCurrentYearTotal = 
				getPermissionAccruedYear(year, Optional.<LocalDate>absent());
		this.vacationDaysCurrentYearTotal = 
				getVacationAccruedYear(year, Optional.<LocalDate>absent());
		
		//(6) Calcolo ferie e permessi non ancora utilizzati per l'anno corrente e per l'anno precedente 
		// (sono funzione di quanto calcolato precedentemente)
		
		//Anno passato
		if(this.accruedDate.isBefore(dateExpireLastYear) || !considerExpireLastYear){
			this.vacationDaysLastYearNotYetUsed = this.vacationDaysLastYearAccrued
					- this.vacationDaysLastYearUsed;
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
					- this.vacationDaysCurrentYearUsed;
			this.persmissionNotYetUsed = this.permissionCurrentYearAccrued
					- this.permissionUsed;
			
		} else {
			//per gli indeterminati le considero tutte (è più improbabile....)
			this.vacationDaysCurrentYearNotYetUsed = this.vacationDaysCurrentYearTotal
					- this.vacationDaysCurrentYearUsed;
			this.persmissionNotYetUsed = this.permissionCurrentYearTotal
					- this.permissionUsed;
		}

	}
	
	public List<Absence> listVacationCurrentYearUsed() {
		List<Absence> absences = Lists.newArrayList();
		absences.addAll(list32RequestYear);
		absences.addAll(list31NextYear);
		absences.addAll(list37NextYear);
		if (this.contract.sourceDate != null && 
				this.contract.sourceDate.getYear() == year) {
			vacationDaysLastYearUsed += this.contract.sourceVacationLastYearUsed;
			vacationDaysCurrentYearUsed += this.contract.sourceVacationCurrentYearUsed;
			permissionUsed += this.contract.sourcePermissionUsed;
		}
		
		return absences;
	}
	
	public List<Absence> listVacationLastYearUsed() {
		List<Absence> absences = Lists.newArrayList();
		absences.addAll(list32PreviouYear);
		absences.addAll(list31RequestYear);
		absences.addAll(list37RequestYear);
		return absences;
	}

	public List<Absence> listPermissionUsed() {
		return list94RequestYear;
	}
	
	public int sourceVacationCurrentYearUsed() {
		if (this.contract.sourceDate != null && 
				this.contract.sourceDate.getYear() == year) {
			return this.contract.sourceVacationCurrentYearUsed;
		}
		return 0;
	}
	
	public int sourceVacationLastYearUsed() {
		if (this.contract.sourceDate != null && 
				this.contract.sourceDate.getYear() == year) {
			return this.contract.sourceVacationLastYearUsed;
		}
		return 0;
	}
	
	public int sourcePermissionUsed() {
		if (this.contract.sourceDate != null && 
				this.contract.sourceDate.getYear() == year) {
			return this.contract.sourcePermissionUsed;
		}
		return 0;
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
			if(vp.vacationCode.equals("21+3") || vp.vacationCode.description.equals("22+3")){
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
	private int getVacationAccruedYear(int year, Optional<LocalDate> accruedDate) {

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
						- filterAbsences(postPartum, intersection);
				
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
				if(vp.vacationCode.description.equals("22+3")){
					vacationDays = vacationDays + VacationsPermissionsDaysAccrued
							.converWorkDaysToVacationDaysPartTimeMoreThanThreeYears(
							days);
				}
			}
		}

		//FIXME decidere se deve essere un parametro di configurazione
		if (vacationDays > 28) {
			vacationDays = 28;
		}

		return vacationDays;

	}
	
	private int filterAbsences(List<Absence> absences, DateInterval interval) {
		int count = 0;
		for(Absence ab : absences) {
			if(DateUtility.isDateIntoInterval(ab.personDay.date, interval)) {
				count++;
			}
		}
		return count;
	}
	
	private void initDataStructures() {
		
		// Gli intervalli su cui predere le assenze nel db
		this.previousYearInterval = DateUtility
				.intervalIntersection(wcontract.getContractDatabaseInterval(),
						new DateInterval(new LocalDate(this.year-1,1,1), 
								new LocalDate(this.year-1,12,31)));
		this.requestYearInterval = DateUtility
				.intervalIntersection(wcontract.getContractDatabaseInterval(),
						new DateInterval(new LocalDate(this.year,1,1), 
								new LocalDate(this.year,12,31)));
		this.nextYearInterval = DateUtility
				.intervalIntersection(wcontract.getContractDatabaseInterval(), 
						new DateInterval(new LocalDate(this.year+1,1,1), 
								new LocalDate(this.year+1,12,31)));
		
		// Il contratto deve essere attivo nell'anno...
		Preconditions.checkNotNull(requestYearInterval);
		LocalDate dateFrom = requestYearInterval.getBegin();
		LocalDate dateTo = requestYearInterval.getEnd();
		if(previousYearInterval != null) {
			dateFrom = previousYearInterval.getBegin();
		}
		if(nextYearInterval != null) {
			dateTo = nextYearInterval.getEnd();
		}
		
		// Le assenze
		List<Absence> absencesForVacationsRecap = absenceDao
				.getAbsencesInCodeList(person, dateFrom, dateTo,
						absenceTypeManager.codesForVacations(), true);
		
		AbsenceType ab32  = absenceTypeManager.getAbsenceType(AbsenceTypeMapping.FERIE_ANNO_CORRENTE.getCode());
		AbsenceType ab31  = absenceTypeManager.getAbsenceType(AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE.getCode());
		AbsenceType ab37  = absenceTypeManager.getAbsenceType(AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE_DOPO_31_08.getCode());
		AbsenceType ab94  = absenceTypeManager.getAbsenceType(AbsenceTypeMapping.FESTIVITA_SOPPRESSE.getCode());
		
		
		for(Absence ab : absencesForVacationsRecap) {
			
			int abYear = ab.personDay.date.getYear();
			
			//32
			if (ab.absenceType.id.equals(ab32.id)) {
				if(abYear == year - 1) {
					list32PreviouYear.add(ab);
				} else if (abYear == year){
					list32RequestYear.add(ab);
				} 
				continue;
			}
			//31
			if (ab.absenceType.id.equals(ab31.id)) {
				if(abYear == year) {
					list31RequestYear.add(ab);
				} else if (abYear == year + 1){
					list31NextYear.add(ab);
				}
				continue;
			}
			//94
			if (ab.absenceType.id.equals(ab94.id)) {
				if(abYear == year) {
					list94RequestYear.add(ab);
				}
			}
			//37
			if (ab.absenceType.id.equals(ab37.id)) {
				if(abYear == year) {
					list37RequestYear.add(ab);
				} else if (abYear == year + 1){
					list37NextYear.add(ab);
				}
				continue;
			} 
			//Post Partum
			postPartum.add(ab);
		}
		
		//Vacation Last Year Expired  
		this.dateExpireLastYear = vacationManager
				.vacationsLastYearExpireDate(year, this.person.office);
		this.isExpireLastYear = false;
		if( this.year < LocalDate.now().getYear() ) {		
			this.isExpireLastYear = true;
		} else if( this.year == LocalDate.now().getYear() 
				&& accruedDate.isAfter(dateExpireLastYear)) {	
			this.isExpireLastYear = true;
		}

		//Contract Expire Before End Of Year / Active After Begin Of Year
		LocalDate startRequestYear = new LocalDate(this.year, 1, 1);
		LocalDate endRequestYear = new LocalDate(this.year, 12, 31);
		if(this.activeContractInterval.getEnd().isBefore(endRequestYear)) {
			this.isExpireBeforeEndYear = true;
		}
		if(this.activeContractInterval.getBegin().isAfter(startRequestYear)) {
			this.isActiveAfterBeginYear = true;
		}
	}

	
}
