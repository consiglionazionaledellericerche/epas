package models;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.PersonUtility;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;

import models.base.BaseModel;
import models.personalMonthSituation.CalcoloSituazioneAnnualePersona;
import models.personalMonthSituation.Mese;
import models.rendering.VacationsRecap;

import org.hibernate.annotations.Type;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import play.Logger;
import play.data.validation.Required;


/**
 * 
 * @author dario
 *
 * il contratto non è gestito direttamente da questa applicazione ma le sue informazioni
 * sono prelevate da un altro servizio
 */

@Entity
@Table(name="contracts")
public class Contract extends BaseModel {
	
	private static final long serialVersionUID = -4472102414284745470L;

	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	@Column(name="source_date")
	public LocalDate sourceDate = null;
	
	@Column(name="source_vacation_last_year_used")
	public Integer sourceVacationLastYearUsed = null;
	
	@Column(name="source_vacation_current_year_used")
	public Integer sourceVacationCurrentYearUsed = null;
	
	@Column(name="source_permission_used")
	public Integer sourcePermissionUsed = null;
	
	@Column(name="source_recovery_day_used")
	public Integer sourceRecoveryDayUsed = null;
	
	@Column(name="source_remaining_minutes_last_year")
	public Integer sourceRemainingMinutesLastYear = null;
	
	@Column(name="source_remaining_minutes_current_year")
	public Integer sourceRemainingMinutesCurrentYear = null;

	


	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="person_id")
	public Person person;
	
	@OneToMany(mappedBy="contract", fetch=FetchType.LAZY, cascade = CascadeType.REMOVE)
	@OrderBy("beginFrom")
	public Set<VacationPeriod> vacationPeriods = Sets.newHashSet();
	
	@OneToMany(mappedBy="contract", fetch=FetchType.LAZY, cascade = CascadeType.REMOVE)
	public List<ContractYearRecap> recapPeriods;

	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	@Column(name="begin_contract")
	public LocalDate beginContract;

	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	@Column(name="expire_contract")
	public LocalDate expireContract;

	//data di termine contratto in casi di licenziamento, pensione, morte, ecc ecc...
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	@Column(name="end_contract")
	public LocalDate endContract;
	
		
	@NotAudited
	@OneToMany(mappedBy = "contract", fetch=FetchType.LAZY, cascade = {CascadeType.REMOVE})
	@OrderBy("beginDate")
	public Set<ContractWorkingTimeType> contractWorkingTimeType = Sets.newHashSet();
	
	
	@NotAudited
	@OneToMany(mappedBy="contract")
	@OrderBy("startFrom")
	public Set<ContractStampProfile> contractStampProfile = Sets.newHashSet();
	

	@Transient
	private List<ContractWorkingTimeType> contractWorkingTimeTypeAsList;
	
	
	//TODO eliminare e configurare yaml
	public void setBeginContract(String date){
		this.beginContract = new LocalDate(date);
	}
	//TODO eliminare e configurare yaml
	public void setEndContract(String date){
		this.endContract = new LocalDate(date);
	}
	//TODO eliminare e configurare yaml
	public void setExpireContract(String date){
		this.expireContract = new LocalDate(date);
	}
	
	public void setSourceDate(String date){
		this.sourceDate = new LocalDate(date);
	}
	
	/**
	 * I contratti con onCertificate = true sono quelli dei dipendenti CNR e 
	 * corrispondono a quelli con l'obbligo dell'attestato di presenza 
	 * da inviare a Roma
	 */
	@Required
	public boolean onCertificate = false;

	@Transient
	public boolean isValidContract(){
		LocalDate date = new LocalDate();
		return endContract==null && beginContract.isBefore(date) && expireContract.isAfter(date);

	}

	@Override
	public String toString() {
		return String.format("Contract[%d] - person.id = %d, beginContract = %s, expireContract = %s, endContract = %s",
				id, person.id, beginContract, expireContract, endContract);
	}

	@Transient
	public List<ContractWorkingTimeType> getContractWorkingTimeTypeAsList() {
		
		return Lists.newArrayList(this.contractWorkingTimeType);
	}
	
	
	@Transient
	public List<ContractStampProfile> getContractStampProfileAsList() {
		
		return Lists.newArrayList(this.contractStampProfile);
	}
	
	@Transient
	public ContractStampProfile getContractStampProfile(LocalDate date) {
		
		for(ContractStampProfile csp : this.contractStampProfile) {
			DateInterval interval = new DateInterval(csp.startFrom, csp.endTo);
			if(DateUtility.isDateIntoInterval(date, interval))
				return csp;
			
		}
		return null;
	}
	
	@Transient
	public ContractStampProfile getCurrentContractStampProfile() {
		
		return getContractStampProfile(LocalDate.now());
	}
	
	
	
	/**
	 * @param contract
	 * @return il vacation period associato al contratto con al suo interno la data di oggi
	 */
	public VacationPeriod getCurrentVacationPeriod()
	{
		for(VacationPeriod vp : this.vacationPeriods) {

			LocalDate now = new LocalDate();

			if(DateUtility.isDateIntoInterval(now, new DateInterval(vp.beginFrom, vp.endTo)))
				return vp;
		}
		return null;
	}
	
	/**
	 * @param date
	 * @return il periodo di validità del WorkingTimeType per il contratto alla data passata come argomento
	 */
	public ContractWorkingTimeType getContractWorkingTimeType(LocalDate date) {
		
		for(ContractWorkingTimeType cwtt: this.contractWorkingTimeType) {
			
			if(DateUtility.isDateIntoInterval(date, new DateInterval(cwtt.beginDate, cwtt.endDate) ))
				return cwtt;
		}
		return null;
	}
		

	/**
	 * @param contract
	 * @return i vacation period associati al contratto, ordinati in ordine crescente per data inizio
	 * 		 	null in caso di vacation period inesistente
	 */
	public List<VacationPeriod> getContractVacationPeriods()
	{
		//vacation period piu' recente per la persona
		List<VacationPeriod> vpList = VacationPeriod.find(  "SELECT vp "
				+ "FROM VacationPeriod vp "
				+ "WHERE vp.contract = ? "
				+ "ORDER BY vp.beginFrom",
				this).fetch();

		//se il piano ferie associato al contratto non esiste 
		if(vpList==null)
		{
			Logger.debug("CurrentPersonVacationPeriod: il vacation period è inesistente");
			return null;
		}


		return vpList;
	}

	/**
	 * Costruisce la struttura valida dei vacation period associati al contratto.
	 */
	public void setVacationPeriods(){


		//Distruggo i vacation period precedenti
		if(this.vacationPeriods != null){
			for(VacationPeriod vp : this.vacationPeriods)
			{
				vp.delete();
			}
		}

		//Contratto terminato nessun vacation period
		if(this.endContract!=null)
		{
			this.save();
			return;
		}

		//Tempo indeterminato, creo due vacatio 3 anni più infinito
		if(this.expireContract == null)
		{

			VacationPeriod first = new VacationPeriod();
			first.beginFrom = this.beginContract;
			first.endTo = this.beginContract.plusYears(3).minusDays(1);
			first.vacationCode = VacationCode.find("Select code from VacationCode code where code.description = ?", "26+4").first();
			first.contract = this;
			first.save();
			VacationPeriod second = new VacationPeriod();
			second.beginFrom = this.beginContract.plusYears(3);
			second.endTo = null;
			second.vacationCode = VacationCode.find("Select code from VacationCode code where code.description = ?", "28+4").first();
			second.contract =this;
			second.save();
			this.save();
			return;
		}

		//Tempo determinato più lungo di 3 anni
		if(this.expireContract.isAfter(this.beginContract.plusYears(3).minusDays(1))){
			VacationPeriod first = new VacationPeriod();
			first.beginFrom = this.beginContract;
			first.endTo = this.beginContract.plusYears(3).minusDays(1);
			first.vacationCode = VacationCode.find("Select code from VacationCode code where code.description = ?", "26+4").first();
			first.contract = this;
			first.save();
			VacationPeriod second = new VacationPeriod();
			second.beginFrom = this.beginContract.plusYears(3);
			second.endTo = this.expireContract;
			second.vacationCode = VacationCode.find("Select code from VacationCode code where code.description = ?", "28+4").first();
			second.contract =this;
			second.save();
			this.save();
			return;
		}

		//Tempo determinato più corto di 3 anni
		VacationPeriod first = new VacationPeriod();
		first.beginFrom = this.beginContract;
		first.endTo = this.expireContract;
		first.contract = this;
		first.vacationCode = VacationCode.find("Select code from VacationCode code where code.description = ?", "26+4").first();
		first.save();
		this.save();
	}
	
	/**
	 * Quando vengono modificate le date di inizio o fine del contratto occorre rivedere la struttura dei periodi di tipo orario.
	 * 1)Eliminare i periodi non più appartenenti al contratto
	 * 2)Modificare la data di inizio del primo periodo se è cambiata la data di inizio del contratto
	 * 3)Modificare la data di fine dell'ultimo periodo se è cambiata la data di fine del contratto
	 * 
	 */
	public void updateContractWorkingTimeType()
	{
		//Aggiornare i periodi workingTimeType
		//1) Cancello quelli che non appartengono più a contract
		List<ContractWorkingTimeType> toDelete = new ArrayList<ContractWorkingTimeType>();
		for(ContractWorkingTimeType cwtt : this.contractWorkingTimeType)
		{
			DateInterval cwttInterval = new DateInterval(cwtt.beginDate, cwtt.endDate);
			if(DateUtility.intervalIntersection(this.getContractDateInterval(), cwttInterval) == null)
			{
				toDelete.add(cwtt);
			}
		}
		for(ContractWorkingTimeType cwtt : toDelete)
		{
			cwtt.delete();
			this.contractWorkingTimeType.remove(cwtt);
			this.save();
		}
		
		//Conversione a List per avere il metodo get()
		List<ContractWorkingTimeType> cwttList = Lists.newArrayList(contractWorkingTimeType);
						
		//Sistemo il primo		
		ContractWorkingTimeType first = cwttList.get(0);
		first.beginDate = this.getContractDateInterval().getBegin();
		first.save();
		//Sistemo l'ultimo
		ContractWorkingTimeType last = 
				cwttList.get(this.contractWorkingTimeType.size()-1);
		last.endDate = this.getContractDateInterval().getEnd();
		if(DateUtility.isInfinity(last.endDate))
			last.endDate = null;
		last.save();
		this.save();
	}
	
	/**
	 * Quando vengono modificate le date di inizio o fine del contratto occorre rivedere la struttura dei periodi di stampProfile.
	 * 1)Eliminare i periodi non più appartenenti al contratto
	 * 2)Modificare la data di inizio del primo periodo se è cambiata la data di inizio del contratto
	 * 3)Modificare la data di fine dell'ultimo periodo se è cambiata la data di fine del contratto
	 * 
	 */
	public void updateContractStampProfile()
	{
		//Aggiornare i periodi stampProfile
		//1) Cancello quelli che non appartengono più a contract
		List<ContractStampProfile> toDelete = new ArrayList<ContractStampProfile>();
		for(ContractStampProfile csp : this.contractStampProfile)
		{
			DateInterval cspInterval = new DateInterval(csp.startFrom, csp.endTo);
			if(DateUtility.intervalIntersection(this.getContractDateInterval(), cspInterval) == null)
			{
				toDelete.add(csp);
			}
		}
		for(ContractStampProfile csp : toDelete)
		{
			csp.delete();
			this.contractWorkingTimeType.remove(csp);
			this.save();
		}
		
		//Conversione a List per avere il metodo get()
		List<ContractStampProfile> cspList = Lists.newArrayList(contractStampProfile);
						
		//Sistemo il primo		
		ContractStampProfile first = cspList.get(0);
		first.startFrom = this.getContractDateInterval().getBegin();
		first.save();
		//Sistemo l'ultimo
		ContractStampProfile last = 
				cspList.get(this.contractStampProfile.size()-1);
		last.endTo = this.getContractDateInterval().getEnd();
		if(DateUtility.isInfinity(last.endTo))
			last.endTo = null;
		last.save();
		this.save();
	}

	/**
	 * Utilizza la libreria DateUtils per costruire l'intervallo attivo per il contratto.
	 * @return
	 */
	public DateInterval getContractDateInterval()
	{
		DateInterval contractInterval;
		if(this.endContract!=null)
			contractInterval = new DateInterval(this.beginContract, this.endContract);
		else
			contractInterval = new DateInterval(this.beginContract, this.expireContract);
		return contractInterval;
	}
	
	/**
	 * Ritorna l'intervallo valido ePAS per il contratto. (scarto la parte precedente a source contract se definita)
	 * @return
	 */
	public DateInterval getContractDatabaseDateInterval() {
		
		if(this.sourceDate != null && this.sourceDate.isAfter(this.beginContract)) {
			
			DateInterval contractInterval;
			if(this.endContract!=null)
				contractInterval = new DateInterval(this.sourceDate, this.endContract);
			else
				contractInterval = new DateInterval(this.sourceDate, this.expireContract);
			return contractInterval;
		}
		
		return this.getContractDateInterval();
		
	}

	
	/**
	 * Ritorna il riepilogo annule del contatto.
	 * @param year
	 * @return
	 */
	public ContractYearRecap getContractYearRecap(int year)
	{
		for(ContractYearRecap cyr : this.recapPeriods)
		{
			if(cyr.year==year)
				return cyr;
		}
		return null;
			
	}
	
	/**
	 * Computa da zero i riepiloghi annuali del contratto. Cancella i riepiloghi precedenti sovrascrivendoli con i nuovi calcoli.
	 */
	public void buildContractYearRecap()
	{
		Logger.info("PopulateContractYearRecap %s %s contract id = %s", this.person.name, this.person.surname, this.id);
		//Distruggere quello che c'è prima (adesso in fase di sviluppo)
		
		while(this.recapPeriods.size()>0)
		{
			ContractYearRecap yearRecap = this.recapPeriods.get(0);
			this.recapPeriods.remove(yearRecap);
			yearRecap.delete();
			this.save();
			
		}
		
		this.recapPeriods = new ArrayList<ContractYearRecap>();
		this.save();
		
		
		//Controllo se ho sufficienti dati
		
		String dateInitUse = ConfGeneral.getFieldValue("init_use_program", person.office);
		LocalDate initUse = new LocalDate(dateInitUse);
		if(this.sourceDate!=null)
			initUse = sourceDate.plusDays(1);
		DateInterval personDatabaseInterval = new DateInterval(initUse, new LocalDate());
		DateInterval contractInterval = this.getContractDateInterval();

		//Se intersezione fra contratto e dati utili database vuota non costruisco alcun contractYearRecap
		if(DateUtility.intervalIntersection(contractInterval, personDatabaseInterval)==null)
			return;

		int yearToCompute = this.beginContract.getYear();
		
		//verifico quanta informazione ho sul contratto
		if(contractInterval.getBegin().isBefore(personDatabaseInterval.getBegin()))
		{
			//contratto non interamente contenuto nel database (serve sourceContract)
			if(this.sourceDate==null)
				return;
			yearToCompute = this.populateContractYearFromSource();
		}
		
		int currentYear = new LocalDate().getYear();
		if(currentYear>contractInterval.getEnd().getYear())
			currentYear = contractInterval.getEnd().getYear();
		while(yearToCompute<currentYear)
		{
			Logger.debug("yearToCompute %s", yearToCompute);
			ContractYearRecap cyr = new ContractYearRecap();
			cyr.year = yearToCompute;
			cyr.contract = this;
			
			//FERIE E PERMESSI
			VacationsRecap vacationRecap = new VacationsRecap(this.person, yearToCompute, this, new LocalDate(), true);
			cyr.vacationLastYearUsed = vacationRecap.vacationDaysLastYearUsed.size();
			cyr.vacationCurrentYearUsed = vacationRecap.vacationDaysCurrentYearUsed.size();
			cyr.permissionUsed = vacationRecap.permissionUsed.size();
			
			//RESIDUI
			CalcoloSituazioneAnnualePersona csap = new CalcoloSituazioneAnnualePersona(this, yearToCompute, new LocalDate().minusDays(1));
			Mese lastComputedMonthInYear;
			if(yearToCompute!=currentYear)
				lastComputedMonthInYear = csap.getMese(yearToCompute, 12);
			else
				lastComputedMonthInYear = csap.getMese(yearToCompute, new LocalDate().getMonthOfYear());
			
			cyr.remainingMinutesLastYear = lastComputedMonthInYear.monteOreAnnoPassato;
			cyr.remainingMinutesCurrentYear = lastComputedMonthInYear.monteOreAnnoCorrente;
			
			//RIPOSI COMPENSATIVI
			//TODO la logica che persiste il dato sui riposi compensativi utilizzati deve essere ancora implementata in quanto non banale.
			//I riposi compensativi utilizzati sono in funzione del contratto?
			//cyr.recoveryDayUsed = PersonUtility.numberOfCompensatoryRestUntilToday(this.person, yearToCompute, 12);
			
			cyr.save();
			this.recapPeriods.add(cyr);
			this.save();
			
			yearToCompute++;
		}
		
	}
	
	
	/**
	 * Costruisce il contractYearRecap se contract.SourceDate è l'ultimo giorno dell'anno.
	 * @return l'anno di cui si deve costruire il prossimo contractYearRecap
	 */
	public int populateContractYearFromSource()
	{
		//Caso semplice source riepilogo dell'anno
		LocalDate lastDayInYear = new LocalDate(this.sourceDate.getYear(), 12, 31);
		if(lastDayInYear.isEqual(this.sourceDate))
		{
			int yearToCompute = this.sourceDate.getYear();
			ContractYearRecap cyr = new ContractYearRecap();
			cyr.year = yearToCompute;
			cyr.contract = this;
			cyr.remainingMinutesCurrentYear = this.sourceRemainingMinutesCurrentYear;
			cyr.remainingMinutesLastYear = this.sourceRemainingMinutesLastYear;
			cyr.vacationLastYearUsed = this.sourceVacationLastYearUsed;
			cyr.vacationCurrentYearUsed = this.sourceVacationCurrentYearUsed;
			cyr.recoveryDayUsed = this.sourceRecoveryDayUsed;
			cyr.permissionUsed = this.sourcePermissionUsed;
			cyr.save();
			this.recapPeriods.add(cyr);
			this.save();
			return yearToCompute+1;
		}

		//Nel caso in cui non sia l'ultimo giorno dell'anno e source cade nell'anno attuale 
		//non devo calcolare alcun riepilogo
		if(this.sourceDate != null && this.sourceDate.getYear() == LocalDate.now().getYear())
			return LocalDate.now().getYear();
		
		//Caso complesso, TODO vedere (dopo che ci sono i test) se creando il VacationRecap si ottengono le stesse informazioni
		AbsenceType ab31 = AbsenceType.getAbsenceTypeByCode("31");
		AbsenceType ab32 = AbsenceType.getAbsenceTypeByCode("32");
		AbsenceType ab37 = AbsenceType.getAbsenceTypeByCode("37");
		AbsenceType ab94 = AbsenceType.getAbsenceTypeByCode("94");
		DateInterval yearInterSource = new DateInterval(this.sourceDate.plusDays(1), lastDayInYear);
		List<Absence> abs32 = VacationsRecap.getVacationDays(yearInterSource, this, ab32);
		List<Absence> abs31 = VacationsRecap.getVacationDays(yearInterSource, this, ab31);
		List<Absence> abs37 = VacationsRecap.getVacationDays(yearInterSource, this, ab37);
		List<Absence> abs94 = VacationsRecap.getVacationDays(yearInterSource, this, ab94);
		int yearToCompute = this.sourceDate.getYear();
		ContractYearRecap cyr = new ContractYearRecap();
		cyr.year = yearToCompute;
		cyr.contract = this;
		cyr.vacationLastYearUsed = this.sourceVacationLastYearUsed + abs31.size() + abs37.size();
		cyr.vacationCurrentYearUsed = this.sourceVacationCurrentYearUsed + abs32.size();
		cyr.permissionUsed = this.sourcePermissionUsed + abs94.size();
		CalcoloSituazioneAnnualePersona csap = new CalcoloSituazioneAnnualePersona(this, yearToCompute, new LocalDate().minusDays(1));
		Mese december = csap.getMese(yearToCompute, 12);
		cyr.remainingMinutesCurrentYear = december.monteOreAnnoCorrente;
		cyr.remainingMinutesLastYear = december.monteOreAnnoPassato;
		cyr.save();
		this.recapPeriods.add(cyr);
		this.save();
		return this.sourceDate.getYear()+1;
		
	}
	
	
	
	/**
	 * True se il contratto è l'ultimo contratto per mese e anno selezionati.
	 * @param month
	 * @param year
	 * @return
	 */
	public boolean isLastInMonth(Integer month, Integer year)
	{
		List<Contract> contractInMonth = this.person.getMonthContracts(month, year);
		if(contractInMonth.size()==0)
			return false;
		if(contractInMonth.get(contractInMonth.size()-1).id.equals(this.id))
			return true;
		else
			return false;
	}
	
	
	/**
	 * True se il contratto non si interseca con nessun altro contratto per la persona. False altrimenti
	 * @return
	 */
	public boolean isProperContract() {

		DateInterval contractInterval = this.getContractDateInterval();
		for(Contract c : person.contracts) {
			
			if(this.id != null && c.id.equals(this.id)) {
				continue;
			}
			
			if(DateUtility.intervalIntersection(contractInterval, c.getContractDateInterval()) != null) {
				return false;
			}
		}
		return true;
	}
	
	/**
	
	 */
	
	/**
	 * Fix definitivo da dateFrom a dateTo per l'intero contratto. (ricalca il flusso di fixPersonSituation).
	 *  
	 * 1) CheckHistoryError 
	 * 2) Ricalcolo tempi lavoro
	 * 3) Ricalcolo riepiloghi annuali 
	 * @param dateFrom giorno a partire dal quale effettuare il ricalcolo. Se null ricalcola dall'inizio del contratto.
	 * @param dateTo ultimo giorno coinvolto nel ricalcolo. Se null ricalcola fino alla fine del contratto (utile nel caso in cui 
	 * si modifica la data fine che potrebbe non essere persistita)
	 */
	public void recomputeContract(LocalDate dateFrom, LocalDate dateTo) {
		
		// (0) Definisco l'intervallo su cui operare
		// Decido la data inizio
		String dateInitUse = ConfGeneral.getFieldValue("init_use_program", person.office);
		LocalDate initUse = new LocalDate(dateInitUse);
		LocalDate date = this.beginContract;
		if(date.isBefore(initUse))
			date = initUse;
		DateInterval contractInterval = this.getContractDatabaseDateInterval();
		if( dateFrom != null && contractInterval.getBegin().isBefore(dateFrom)) {
			contractInterval = new DateInterval(dateFrom, contractInterval.getEnd());
		}
		// Decido la data di fine
		if(dateTo != null && dateTo.isBefore(contractInterval.getEnd())) {
			contractInterval = new DateInterval(contractInterval.getBegin(), dateTo);
		}
		
		// (1) Porto il db in uno stato consistente costruendo tutti gli eventuali person day mancanti
		LocalDate today = new LocalDate();
		Logger.info("CheckPersonDay (creazione ed history error) DA %s A %s", date, today);
		while(true) {
			Logger.debug("RecomputePopulate %s", date);
			if(!DateUtility.isDateIntoInterval(date, contractInterval)) {
				date = date.plusDays(1);
				continue;
			}
			if(date.isEqual(today))
				break;
			
			PersonUtility.checkPersonDay(person.id, date);
			date = date.plusDays(1);
			
			
		}
		
		
		
		// (2) Ricalcolo i valori dei person day aggregandoli per mese
		LocalDate actualMonth = contractInterval.getBegin().withDayOfMonth(1).minusMonths(1);
		LocalDate endMonth = new LocalDate().withDayOfMonth(1);

		Logger.debug("PopulatePersonDay (ricalcoli ed history error) DA %s A %s", actualMonth, endMonth);
		
		while( !actualMonth.isAfter(endMonth) )
		{
			List<PersonDay> pdList = 
					PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ? order by pd.date", 
					this.person, actualMonth, actualMonth.dayOfMonth().withMaximumValue()).fetch();

			for(PersonDay pd : pdList){
				
				PersonDay pd1 = PersonDay.findById(pd.id);
				Logger.debug("RecomputePopulate %s", pd1.date);
				pd1.populatePersonDay();
			}

			actualMonth = actualMonth.plusMonths(1);
		}

		Logger.info("BuildContractYearRecap");
		//(3) Ricalcolo dei riepiloghi annuali
		this.buildContractYearRecap();
		
		
	}
	
	/**
	 * La lista con tutti i contratti attivi nel periodo selezionato.
	 * @return
	 */
	public static List<Contract> getActiveContractInPeriod(LocalDate begin, LocalDate end) {
		
		//TODO queryDSL
		if(end == null)
			end = new LocalDate(9999,1,1);
		
		List<Contract> activeContract = Contract.find(
				"Select c from Contract c "
										
						//contratto attivo nel periodo
						+ " where ( "
						//caso contratto non terminato
						+ "c.endContract is null and "
							//contratto a tempo indeterminato che si interseca col periodo 
							+ "( (c.expireContract is null and c.beginContract <= ? )"
							+ "or "
							//contratto a tempo determinato che si interseca col periodo (comanda il campo endContract)
							+ "(c.expireContract is not null and c.beginContract <= ? and c.expireContract >= ? ) ) "
						+ "or "
						//caso contratto terminato che si interseca col periodo		
						+ "c.endContract is not null and c.beginContract <= ? and c.endContract >= ? "
						+ ") "
						, end, end, begin, end, begin).fetch();
		
		return activeContract;
		
		
	}
		
}
	
	

