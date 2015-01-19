package manager;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.ArrayList;
import java.util.List;

import manager.recaps.PersonResidualMonthRecap;
import manager.recaps.PersonResidualYearRecap;
import models.Absence;
import models.AbsenceType;
import models.ConfGeneral;
import models.Contract;
import models.ContractYearRecap;
import models.rendering.VacationsRecap;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;

import dao.AbsenceTypeDao;
import play.Logger;

/**
 * 
 * Manager per ContractYearRecap
 * 
 * @author alessandro
 *
 */
public class ContractYearRecapManager {
	
	/**
	 * NB !!!
	 * 
	 * contract.source* (se definiti) contengono la situazione della persona
	 * ALLA FINE della giornata contenuta in contract.sourceDate. I dati del database
	 * significativi sono quindi quelli a partire da contract.sourceDate.plusDay(1).
	 * 
	 * office.init_use_program contiene la data di messa in funzione di ePAS per 
	 * quella sede ovvero il giorno a partire dal quale il database contiene dati
	 * significativi dei dipendenti. 
	 * (Esempio: se in data init_use_program un dipendente non ha timbrature sono -7:12)
	 * 
	 * Pertanto:
	 * 
	 * (1)
	 * Nel caso di dipendente aggiunto contestualmente alla installazione del nuovo ufficio
	 * è auspicabile che contract.sourceDate sia il giorno immediatamente precedente a 
	 * office.init_use_program.
	 * (Ma nulla vieta che sia una data uguale o successiva, come nel caso di tutti quei dipendenti 
	 * aggiunti successivamente per trasferimento. I tal caso la data init_use_program sarà
	 * insignificante per il dipendente).
	 * 
	 * (2)
	 * sourceDate può essere successivo, uguale o immediatamente precedente a init_use_program MA NON
	 * precedente per più di un giorno a init_use_program.
	 * Questo evento andrebbe segnalato come mancanza di dati per il dipendente.
	 * 
	 * 
	 * Poichè in contract.source* mancano ancora dei campi significativi per la chiusura del
	 * mese per l'invio degli attestati (i buoni maturati nel mese per i giorni fino a sourceDate),
	 * per adesso l'unica maniera per avere una installazione pulita della persona è far sì che:
	 * 1) init_use_program sia il primo giorno del mese.
	 * 2) contract.sourceDate sia l'ultimo giorno del mese. 
	 * Questa regola potrebbe salvarci da eventuali ulteriori mancanze future.
	 * 
	 */

	/**
	 * Computa da zero i riepiloghi annuali del contratto. 
	 * Cancella i riepiloghi precedenti sovrascrivendoli con i nuovi calcoli.
	 * 
	 * @param contract
	 */
	public static void buildContractYearRecap(Contract contract)
	{
		Logger.info("PopulateContractYearRecap %s %s contract id = %s", contract.person.name, contract.person.surname, contract.id);
		//Distruggere quello che c'è prima (adesso in fase di sviluppo)
		
		while(contract.recapPeriods.size()>0)
		{
			ContractYearRecap yearRecap = contract.recapPeriods.get(0);
			contract.recapPeriods.remove(yearRecap);
			yearRecap.delete();
			contract.save();
			
		}
		
		contract.recapPeriods = new ArrayList<ContractYearRecap>();
		contract.save();
		
		
		//Controllo se ho sufficienti dati
		
		String dateInitUse = ConfGeneral.getFieldValue("init_use_program", contract.person.office);
		LocalDate initUse = new LocalDate(dateInitUse);
		if(contract.sourceDate!=null)
			initUse = contract.sourceDate.plusDays(1);
		DateInterval personDatabaseInterval = new DateInterval(initUse, new LocalDate());
		DateInterval contractInterval = contract.getContractDateInterval();

		//Se intersezione fra contratto e dati utili database vuota non costruisco alcun contractYearRecap
		if(DateUtility.intervalIntersection(contractInterval, personDatabaseInterval)==null)
			return;

		int yearToCompute = contract.beginContract.getYear();
		
		//verifico quanta informazione ho sul contratto
		if(contractInterval.getBegin().isBefore(personDatabaseInterval.getBegin()))
		{
			//contratto non interamente contenuto nel database (serve sourceContract)
			if(contract.sourceDate==null)
				return;
			yearToCompute = populateContractYearFromSource(contract);
		}
		
		int currentYear = new LocalDate().getYear();
		if(currentYear>contractInterval.getEnd().getYear())
			currentYear = contractInterval.getEnd().getYear();
		while(yearToCompute<currentYear)
		{
			Logger.debug("yearToCompute %s", yearToCompute);
			ContractYearRecap cyr = new ContractYearRecap();
			cyr.year = yearToCompute;
			cyr.contract = contract;
			
			//FERIE E PERMESSI
			VacationsRecap vacationRecap = VacationsRecap.Factory.build(contract.person, yearToCompute, Optional.of(contract), new LocalDate(), true);
			cyr.vacationLastYearUsed = vacationRecap.vacationDaysLastYearUsed.size();
			cyr.vacationCurrentYearUsed = vacationRecap.vacationDaysCurrentYearUsed.size();
			cyr.permissionUsed = vacationRecap.permissionUsed.size();
			
			//RESIDUI
			PersonResidualYearRecap csap = 
					PersonResidualYearRecap.factory(contract, yearToCompute, new LocalDate().minusDays(1));
			PersonResidualMonthRecap lastComputedMonthInYear;
			if(yearToCompute!=currentYear)
				lastComputedMonthInYear = csap.getMese(12);
			else
				lastComputedMonthInYear = csap.getMese(new LocalDate().getMonthOfYear());
			
			cyr.remainingMinutesLastYear = lastComputedMonthInYear.monteOreAnnoPassato;
			cyr.remainingMinutesCurrentYear = lastComputedMonthInYear.monteOreAnnoCorrente;
			cyr.remainingMealTickets = lastComputedMonthInYear.buoniPastoResidui;
			
			//RIPOSI COMPENSATIVI
			//TODO la logica che persiste il dato sui riposi compensativi utilizzati deve essere ancora implementata in quanto non banale.
			//I riposi compensativi utilizzati sono in funzione del contratto?
			//cyr.recoveryDayUsed = PersonUtility.numberOfCompensatoryRestUntilToday(this.person, yearToCompute, 12);
			
			cyr.save();
			contract.recapPeriods.add(cyr);
			contract.save();
			
			yearToCompute++;
		}
		
	}
	
	/**
	 * Costruisce il contractYearRecap da contract.SourceDate.
	 * 1) Se sourceDate è l'ultimo giorno dell'anno costruisce il riepilogo 
	 *    copiando le informazioni in esso contenute.
	 * 2) Se sourceDate non è l'ultimo giorno dell'anno e si riferisce all'anno corrente
	 *    allora non si deve creare alcun riepilogo.   
	 * 3) Se sourceDate non è l'ultimo giorno dell'anno e si riferisce ad un anno passato
	 *    costruisce il riepilogo andando a combinare le informazioni presenti in sourceContract
	 *    e nel database (a partire dal giorno successivo a sourceDate).
	 *    
	 * @return l'anno di cui si deve costruire il prossimo contractYearRecap
	 */
	private static int populateContractYearFromSource(Contract contract)
	{
		//Caso semplice source riepilogo dell'anno
		
		LocalDate lastDayInYear = new LocalDate(contract.sourceDate.getYear(), 12, 31);
		if(lastDayInYear.isEqual(contract.sourceDate))
		{
			int yearToCompute = contract.sourceDate.getYear();
			ContractYearRecap cyr = new ContractYearRecap();
			cyr.year = yearToCompute;
			cyr.contract = contract;
			cyr.remainingMinutesCurrentYear = contract.sourceRemainingMinutesCurrentYear;
			cyr.remainingMinutesLastYear = contract.sourceRemainingMinutesLastYear;
			cyr.vacationLastYearUsed = contract.sourceVacationLastYearUsed;
			cyr.vacationCurrentYearUsed = contract.sourceVacationCurrentYearUsed;
			cyr.recoveryDayUsed = contract.sourceRecoveryDayUsed;
			cyr.permissionUsed = contract.sourcePermissionUsed;
			cyr.save();
			contract.recapPeriods.add(cyr);
			contract.save();
			return yearToCompute+1;
		}

		//Nel caso in cui non sia l'ultimo giorno dell'anno e source cade nell'anno attuale 
		//non devo calcolare alcun riepilogo
		if(contract.sourceDate != null && contract.sourceDate.getYear() == LocalDate.now().getYear())
			return LocalDate.now().getYear();
		
		//Caso complesso, TODO vedere (dopo che ci sono i test) se creando il VacationRecap si ottengono le stesse informazioni
		AbsenceType ab31 = AbsenceTypeDao.getAbsenceTypeByCode("31");
		AbsenceType ab32 = AbsenceTypeDao.getAbsenceTypeByCode("32");
		AbsenceType ab37 = AbsenceTypeDao.getAbsenceTypeByCode("37"); 
		AbsenceType ab94 = AbsenceTypeDao.getAbsenceTypeByCode("94"); 
		DateInterval yearInterSource = new DateInterval(contract.sourceDate.plusDays(1), lastDayInYear);
		List<Absence> abs32 = VacationsRecap.getVacationDays(yearInterSource, contract, ab32);
		List<Absence> abs31 = VacationsRecap.getVacationDays(yearInterSource, contract, ab31);
		List<Absence> abs37 = VacationsRecap.getVacationDays(yearInterSource, contract, ab37);
		List<Absence> abs94 = VacationsRecap.getVacationDays(yearInterSource, contract, ab94);
		int yearToCompute = contract.sourceDate.getYear();
		ContractYearRecap cyr = new ContractYearRecap();
		cyr.year = yearToCompute;
		cyr.contract = contract;
		cyr.vacationLastYearUsed = contract.sourceVacationLastYearUsed + abs31.size() + abs37.size();
		cyr.vacationCurrentYearUsed = contract.sourceVacationCurrentYearUsed + abs32.size();
		cyr.permissionUsed = contract.sourcePermissionUsed + abs94.size();
		PersonResidualYearRecap csap = 
				PersonResidualYearRecap.factory(contract, yearToCompute, new LocalDate().minusDays(1));
		PersonResidualMonthRecap december = csap.getMese(12);
		cyr.remainingMinutesCurrentYear = december.monteOreAnnoCorrente;
		cyr.remainingMinutesLastYear = december.monteOreAnnoPassato;
		cyr.save();
		contract.recapPeriods.add(cyr);
		contract.save();
		return contract.sourceDate.getYear()+1;
		
	}

}
