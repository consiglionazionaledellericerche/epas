package manager;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.ArrayList;

import models.ConfGeneral;
import models.Contract;
import models.ContractYearRecap;
import models.personalMonthSituation.CalcoloSituazioneAnnualePersona;
import models.personalMonthSituation.Mese;
import models.rendering.VacationsRecap;

import org.joda.time.LocalDate;

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
			yearToCompute = contract.populateContractYearFromSource();
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
			VacationsRecap vacationRecap = new VacationsRecap(contract.person, yearToCompute, contract, new LocalDate(), true);
			cyr.vacationLastYearUsed = vacationRecap.vacationDaysLastYearUsed.size();
			cyr.vacationCurrentYearUsed = vacationRecap.vacationDaysCurrentYearUsed.size();
			cyr.permissionUsed = vacationRecap.permissionUsed.size();
			
			//RESIDUI
			CalcoloSituazioneAnnualePersona csap = new CalcoloSituazioneAnnualePersona(contract, yearToCompute, new LocalDate().minusDays(1));
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
			contract.recapPeriods.add(cyr);
			contract.save();
			
			yearToCompute++;
		}
		
	}

}
