package dao.wrapper;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.List;

import manager.ConfGeneralManager;
import models.Contract;
import models.ContractMonthRecap;
import models.ContractWorkingTimeType;
import models.enumerate.Parameter;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * @author marco
 *
 */
public class WrapperContract implements IWrapperContract {

	private final Contract value;
	private final ConfGeneralManager confGeneralManager;
	private final IWrapperFactory wrapperFactory;

	@Inject
	WrapperContract(@Assisted Contract contract,
			ConfGeneralManager confGeneralManager, IWrapperFactory wrapperFactory) {
		value = contract;
		this.confGeneralManager = confGeneralManager;
		this.wrapperFactory = wrapperFactory;
	}

	@Override
	public Contract getValue() {
		return value;
	}

	/**
	 * True se il contratto è l'ultimo contratto per mese e anno selezionati.
	 * @param month
	 * @param year
	 * @return
	 */
	@Override
	public boolean isLastInMonth(int month, int year) {
		
		DateInterval monthInterval = new DateInterval(new LocalDate(year, month,1), 
				new LocalDate(year, month,1).dayOfMonth().withMaximumValue());
		
		for(Contract contract : value.person.contracts) {
			if(contract.id.equals(value.id)) {
				continue;
			}
			DateInterval cInterval = wrapperFactory.create(contract)
					.getContractDateInterval();
			if (DateUtility.intervalIntersection(monthInterval,cInterval) != null) {
				if(value.beginContract.isBefore(contract.beginContract)) {
					return false; 
				}
			}
		}
		return true;
		
	}

	/**
	 * True se il contratto è a tempo determinato.
	 * 
	 * @return
	 */
	@Override
	public boolean isDefined() {

		return this.value.expireContract != null;
	}

	/**
	 * Conversione della lista dei contractWorkingtimeType da Set a List
	 * @param contract
	 * * @return
	 */
	@Override
	public List<ContractWorkingTimeType> getContractWorkingTimeTypeAsList() {
		return Lists.newArrayList(this.value.contractWorkingTimeType);
	}

	/**
	 * L'intervallo attivo per il contratto.
	 * 
	 * @return
	 */
	@Override
	public DateInterval getContractDateInterval() {
		if (value.endContract != null)
			return new DateInterval(value.beginContract, value.endContract);
		else
			return new DateInterval(value.beginContract, value.expireContract);
	}
	
	/**
	 * L'intervallo dei giorni da considerare per le computazioni nel database ePAS.
	 * 
	 */
	@Override 
	public DateInterval getContractDatabaseInterval() {
		
		// TODO: verificare il funzionamento.
		// Assumo che initUse in configurazione sia ininfluente perchè se definita
		// allora automaticamente deve essere definito sourceContract.
		
		DateInterval contractInterval = getContractDateInterval();
		if (value.sourceDate != null) {
			return new DateInterval(value.sourceDate.plusDays(1),
					contractInterval.getEnd());
		}
		return contractInterval;
	}
	
	/**
	 * Il mese del primo riepilogo esistente per il contratto.
	 * 
	 */
	@Override
	public YearMonth getFirstMonthToRecap() {
		if (value.sourceDate != null) {
			return new YearMonth(value.sourceDate);
		}
		return new YearMonth(value.beginContract);
	}
	
	/**
	 * Il mese dell'ultimo riepilogo esistente per il contratto (al momento 
	 * della chiamata).
	 * 
	 * @return
	 */
	@Override
	public YearMonth getLastMonthToRecap() {
		YearMonth currentMonth = new YearMonth(LocalDate.now());
		YearMonth lastMonth = new YearMonth( getContractDateInterval().getEnd() );
		if ( currentMonth.isAfter(lastMonth) ) {
			return lastMonth;
		}
		return currentMonth;
	}
	
	/**
	 * 
	 * @param yearMonth
	 * @return
	 */
	@Override
	public Optional<ContractMonthRecap> getContractMonthRecap(YearMonth yearMonth) {
		
		for (ContractMonthRecap cmr : value.contractMonthRecaps) {
			if ( cmr.year == yearMonth.getYear() 
					&& cmr.month == yearMonth.getMonthOfYear() ) {
				return Optional.fromNullable(cmr);
			}
		}
		return Optional.absent();
	}
	
	/**
	 * Diagnostiche sul contratto.
	 * 
	 * @return
	 */
	@Override
	public boolean initializationMissing() {
		
		// Se la data di inizio contratto è successiva alla data di utilizzo del 
		// programma allora ci sono problemi di inizializzazione.
		
		LocalDate officeInstallation = 
				new LocalDate(confGeneralManager.getFieldValue(
						Parameter.INIT_USE_PROGRAM, value.person.office));
		
		if( value.beginContract.isBefore(officeInstallation) 
				&& value.sourceDate == null) {
			return true;
		}
		
		return false;
	}
	
	@Override
	public boolean monthRecapMissing(YearMonth yearMonth) {

		if ( getContractMonthRecap(yearMonth).isPresent() ) {
			return false;
		} 
		return true;
	}			
			
	@Override
	public boolean monthRecapMissing() {	

		YearMonth monthToCheck = getFirstMonthToRecap();
		YearMonth nowMonth = YearMonth.now();
		while( !monthToCheck.isAfter( nowMonth)) {
			// FIXME: renderlo efficiente, un dao che li ordina.
			if ( !getContractMonthRecap(monthToCheck).isPresent() ) {
				return true;
			}
			monthToCheck = monthToCheck.plusMonths(1);
		}

		return false;
	}
	
	@Override
	public boolean hasMonthRecapForVacationsRecap(int yearToRecap) {
		
		// se il contratto inizia nell'anno non ho bisogno del recap.
		if (value.beginContract.getYear() == yearToRecap) {
			return true;
		}
		
		// se source date cade nell'anno non ho bisogno del recap.
		if (value.sourceDate != null 
				&& value.sourceDate.getYear() == yearToRecap) {
			return true;
		}
		// Altrimenti ho bisogno del riepilogo finale dell'anno precedente.
		Optional<ContractMonthRecap> yearMonthToCheck = 
				getContractMonthRecap( new YearMonth(yearToRecap-1, 12) );
		
		if( yearMonthToCheck.isPresent() ) {
			return true;
		}
		
		return false;
	}
	
	@Override
	public boolean noRelevant() {
		
		LocalDate officeInstallation = 
				new LocalDate(confGeneralManager.getFieldValue(
						Parameter.INIT_USE_PROGRAM, value.person.office));
		
		if (officeInstallation.isAfter( getContractDateInterval().getEnd() )) {
			return true;
		}
		return false;
	}
	
}
