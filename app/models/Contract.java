package models;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.PersonUtility;

import java.util.ArrayList;
import java.util.List;

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

import models.personalMonthSituation.CalcoloSituazioneAnnualePersona;
import models.personalMonthSituation.Mese;
import models.rendering.VacationsRecap;

import org.hibernate.annotations.Type;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;

import controllers.Persons;
import play.Logger;
import play.data.validation.Required;
import play.db.jpa.JPAPlugin;
import play.db.jpa.Model;

/**
 * 
 * @author dario
 *
 * il contratto non è gestito direttamente da questa applicazione ma le sue informazioni
 * sono prelevate da un altro servizio
 */

@Entity
@Table(name="contracts")
public class Contract extends Model {

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

	
	private static final long serialVersionUID = -4472102414284745470L;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="person_id")
	public Person person;
	
	@OneToMany(mappedBy="contract", fetch=FetchType.LAZY, cascade = CascadeType.REMOVE)
	public List<VacationPeriod> vacationPeriods;
	
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
	public List<ContractWorkingTimeType> contractWorkingTimeType = new ArrayList<ContractWorkingTimeType>();

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

	/**
	 * @param contract
	 * @return il vacation period associato al contratto con al suo interno la data di oggi
	 */
	public VacationPeriod getCurrentVacationPeriod()
	{
		List<VacationPeriod> vpList = this.getContractVacationPeriods();
		for(VacationPeriod vp : vpList) {

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
		//Sistemo il primo
		ContractWorkingTimeType first = this.contractWorkingTimeType.get(0);
		first.beginDate = this.getContractDateInterval().getBegin();
		first.save();
		//Sistemo l'ultimo
		ContractWorkingTimeType last = this.contractWorkingTimeType.get(this.contractWorkingTimeType.size()-1);
		last.endDate = this.getContractDateInterval().getEnd();
		if(DateUtility.isInfinity(last.endDate))
			last.endDate = null;
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
		LocalDate lastDayInYear = new LocalDate(this.sourceDate.getYear(), 12, 31);
		//Caso semplice source riepilogo dell'anno
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
	 * Fix definitivo da dateFrom a oggi per l'intero contratto. (ricalca il flusso di fixPersonSituation).
	 *  
	 * 1) CheckHistoryError 
	 * 2) Ricalcolo tempi lavoro
	 * 3) Ricalcolo riepiloghi annuali 
	 * @param dateFrom giorno a partire dal quale effettuare il ricalcolo. Se null ricalcola dall'inizio del contratto.
	 */
	public void recomputeContract(LocalDate dateFrom) {
		
		// (0) Definisco l'intervallo su cui operare
		String dateInitUse = ConfGeneral.getFieldValue("init_use_program", person.office);
		LocalDate initUse = new LocalDate(dateInitUse);
		LocalDate date = this.beginContract;
		if(date.isBefore(initUse))
			date = initUse;
		DateInterval contractInterval = this.getContractDatabaseDateInterval();
		if( dateFrom != null && contractInterval.getBegin().isBefore(dateFrom)) {
			contractInterval = new DateInterval(dateFrom, contractInterval.getEnd());
		}

		// (1) Porto il db in uno stato consistente costruendo tutti gli eventuali person day mancanti
		Logger.info("CheckPersonDay");
		LocalDate today = new LocalDate();
		while(true) {
			
			PersonUtility.checkPersonDay(person.id, date);
			date = date.plusDays(1);
			if(date.isEqual(today))
				break;
			if(!DateUtility.isDateIntoInterval(date, contractInterval))
				break;
		}
		
		Logger.info("PopulatePersonDay");
		
		// (2) Ricalcolo i valori dei person day aggregandoli per mese
		LocalDate actualMonth = contractInterval.getBegin().withDayOfMonth(1);
		LocalDate endMonth = new LocalDate().withDayOfMonth(1);

		while( !actualMonth.isAfter(endMonth) )
		{

			List<PersonDay> pdList = 
					PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ? order by pd.date", 
					this.person, actualMonth, actualMonth.dayOfMonth().withMaximumValue()).fetch();

			for(PersonDay pd : pdList){
				
				PersonDay pd1 = PersonDay.findById(pd.id);
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
	
	

