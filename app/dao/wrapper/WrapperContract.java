package dao.wrapper;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import manager.ConfGeneralManager;
import models.Contract;
import models.ContractMonthRecap;
import models.ContractWorkingTimeType;
import models.enumerate.Parameter;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import java.util.List;

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
	
	public boolean isActive() {
		return DateUtility.isDateIntoInterval(LocalDate.now(), getContractDateInterval());
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
		if (value.endContract != null) {
			return new DateInterval(value.beginContract, value.endContract);
		} else {
			return new DateInterval(value.beginContract, value.expireContract);
		}
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
		if (value.sourceDateResidual != null) {
			return new DateInterval(value.sourceDateResidual.plusDays(1),
					contractInterval.getEnd());
		}
//		
//		Optional<LocalDate> dateInitUse = confGeneralManager
//				.getLocalDateFieldValue(Parameter.INIT_USE_PROGRAM, value.person.office);
//		if(dateInitUse.isPresent() ) {
//			
//			if(dateInitUse.get().isAfter(contractInterval.getBegin())) {
//				return new DateInterval(dateInitUse.get(), contractInterval.getEnd());
//			}
//		}
		
		return contractInterval;
	}
	
	/**
	 * L'intervallo dei giorni da considerare per le computazioni nel database ePAS.
	 * 
	 */
	@Override 
	public DateInterval getContractDatabaseIntervalForMealTicket() {
		
		DateInterval contractDatebaseInterval = getContractDatabaseInterval();
		if (value.sourceDateMealTicket != null) {
			return new DateInterval(value.sourceDateMealTicket.plusDays(1),
					contractDatebaseInterval.getEnd());
		}
	
		return contractDatebaseInterval;
	}
	
	/**
	 * Il mese del primo riepilogo esistente per il contratto.
	 * absent() se non ci sono i dati per costruire il primo riepilogo. 
	 */
	@Override
	public Optional<YearMonth> getFirstMonthToRecap() {
		
		if( initializationMissing() ) {
			return Optional.<YearMonth>absent();
		}
		if (value.sourceDateResidual != null) {
			return Optional.fromNullable((new YearMonth(value.sourceDateResidual)));
		}
		return Optional.fromNullable(new YearMonth(value.beginContract));
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
		
		LocalDate dateForInit = dateForInitialization();
		
		if(value.sourceDateResidual != null) {
			return false;
		}

		return value.beginContract.isBefore(dateForInit);
	}
	
	/**
	 * La data di inizilizzazione è la successiva fra la creazione della persona
	 * e l'inizio utilizzo del software della sede della persona (che potrebbe 
	 * cambiare a causa del trasferimento).
	 */
	@Override
	public LocalDate dateForInitialization() {
		LocalDate officeInstallation =	new LocalDate(
				confGeneralManager.getFieldValue(Parameter.INIT_USE_PROGRAM,
						value.person.office));
		LocalDate personCreation = new LocalDate(value.person.createdAt);
		LocalDate candidate = value.beginContract;
		
		if(candidate.isBefore(officeInstallation)) {
			candidate = officeInstallation;
		}
		if(candidate.isBefore(personCreation)) {
			candidate = personCreation;
		}
	 
		return candidate;
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

		Optional<YearMonth> monthToCheck = getFirstMonthToRecap();
		if (!monthToCheck.isPresent()) {
			return true;
		}
		YearMonth nowMonth = YearMonth.now();
		while( !monthToCheck.get().isAfter( nowMonth)) {
			// FIXME: renderlo efficiente, un dao che li ordina.
			if ( !getContractMonthRecap(monthToCheck.get()).isPresent() ) {
				return true;
			}
			monthToCheck = Optional.fromNullable(monthToCheck.get().plusMonths(1));
		}

		return false;
	}
	
	@Override
	public boolean hasMonthRecapForVacationsRecap(int yearToRecap) {
		
		// se non ho il contratto inizializzato il riepilogo ferie non esiste
		//o non è veritiero.
		if (initializationMissing()) {
			return false;
		}
		
		// se il contratto inizia nell'anno non ho bisogno del recap.
		if (value.beginContract.getYear() == yearToRecap) {
			return true;
		}
		
		// se source date cade nell'anno non ho bisogno del recap.
		if (value.sourceDateResidual != null 
				&& value.sourceDateResidual.getYear() == yearToRecap) {
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
