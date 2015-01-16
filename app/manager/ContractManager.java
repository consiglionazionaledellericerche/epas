package manager;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.PersonUtility;

import java.util.List;

import models.ConfGeneral;
import models.Contract;
import models.ContractStampProfile;
import models.ContractWorkingTimeType;
import models.PersonDay;
import models.WorkingTimeType;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;

import dao.PersonDayDao;
import dao.WorkingTimeTypeDao;
import play.Logger;

/**
 * 
 * Manager per Contract
 * 
 * @author alessandro
 *
 */
public class ContractManager {
	
	/**
	 * Costruisce in modo controllato tutte le strutture dati associate al contratto
	 * appena creato passato come argomento.
	 * (1) I piani ferie associati al contratto
	 * (2) Il periodo con tipo orario Normale per la durata del contratto
	 * (3) Il periodo con timbratura default impostata a false.
	 * 
	 * @param contract
	 */
	public static void contractProperCreate(Contract contract) {
		
		contract.setVacationPeriods();

		//FIXME deve essere impostato in configurazione l'orario default
		WorkingTimeType wtt = WorkingTimeTypeDao.getWorkingTimeTypeByDescription("Normale");
		
		ContractWorkingTimeType cwtt = new ContractWorkingTimeType();
		cwtt.beginDate = contract.beginContract;
		cwtt.endDate = contract.expireContract;
		cwtt.workingTimeType = wtt;
		cwtt.contract = contract;
		cwtt.save();
		
		ContractStampProfile csp = new ContractStampProfile();
		csp.contract = contract;
		csp.startFrom = contract.beginContract;
		csp.endTo = contract.expireContract;
		csp.fixedworkingtime = false;
		csp.save();
		contract.save();

	}
	
	/**
	 * Ricalcola completamente tutti i dati del contratto da dateFrom a dateTo.
	 *  
	 * 1) CheckHistoryError 
	 * 2) Ricalcolo tempi lavoro
	 * 3) Ricalcolo riepiloghi annuali 
	 * 
	 * @param dateFrom giorno a partire dal quale effettuare il ricalcolo. 
	 *   Se null ricalcola dall'inizio del contratto.
	 *   
	 * @param dateTo ultimo giorno coinvolto nel ricalcolo. 
	 *   Se null ricalcola fino alla fine del contratto (utile nel caso in cui si 
	 *   modifica la data fine che potrebbe non essere persistita)
	 */
	public static void recomputeContract(Contract contract, LocalDate dateFrom, LocalDate dateTo) {
		
		// (0) Definisco l'intervallo su cui operare
		// Decido la data inizio
		String dateInitUse = ConfGeneral.getFieldValue("init_use_program", contract.person.office);
		LocalDate initUse = new LocalDate(dateInitUse);
		LocalDate date = contract.beginContract;
		if(date.isBefore(initUse))
			date = initUse;
		DateInterval contractInterval = contract.getContractDatabaseDateInterval();
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
			
			if(date.isEqual(today))
				break;
			
			if(!DateUtility.isDateIntoInterval(date, contractInterval)) {
				date = date.plusDays(1);
				continue;
			}
			
			PersonUtility.checkPersonDay(contract.person.id, date);
			date = date.plusDays(1);
			
			
		}

		// (2) Ricalcolo i valori dei person day aggregandoli per mese
		LocalDate actualMonth = contractInterval.getBegin().withDayOfMonth(1).minusMonths(1);
		LocalDate endMonth = new LocalDate().withDayOfMonth(1);

		Logger.debug("PopulatePersonDay (ricalcoli ed history error) DA %s A %s", actualMonth, endMonth);
		
		while( !actualMonth.isAfter(endMonth) )
		{
			List<PersonDay> pdList = PersonDayDao.getPersonDayInPeriod(contract.person, actualMonth, Optional.fromNullable(actualMonth.dayOfMonth().withMaximumValue()), true);
//			List<PersonDay> pdList = 
//					PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ? order by pd.date", 
//							contract.person, actualMonth, actualMonth.dayOfMonth().withMaximumValue()).fetch();

			for(PersonDay pd : pdList){
				
				PersonDay pd1 = PersonDayDao.getPersonDayById(pd.id);
				//PersonDay pd1 = PersonDay.findById(pd.id);
				Logger.debug("RecomputePopulate %s", pd1.date);				
				pd1.populatePersonDay();
			}

			actualMonth = actualMonth.plusMonths(1);
		}

		Logger.info("BuildContractYearRecap");
		
		//(3) Ricalcolo dei riepiloghi annuali
		ContractYearRecapManager.buildContractYearRecap(contract);
		
		
	}

}
