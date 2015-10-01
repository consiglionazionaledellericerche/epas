package manager.recaps.personStamping;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import dao.MealTicketDao;
import dao.PersonDayDao;
import dao.wrapper.IWrapperContractMonthRecap;
import dao.wrapper.IWrapperFactory;
import it.cnr.iit.epas.DateUtility;
import manager.ContractMonthRecapManager;
import manager.PersonDayManager;
import manager.PersonManager;
import models.*;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Oggetto che modella il contenuto della vista contenente il tabellone timbrature.
 * Gerarchia:
 * PersonStampingRecap (tabella mese) 
 * 	  -> PersonStampingDayRecap (riga giorno) 
 * 		-> StampingTemplate (singola timbratura)
 * 
 * @author alessandro
 *
 */
public class PersonStampingRecap {

	private static final int MIN_IN_OUT_COLUMN = 2;

	public Person person;
	public int year;
	public int month;
	
	public boolean currentMonth = false;

	//Informazioni sul mese
	public int numberOfCompensatoryRestUntilToday = 0;
	public int numberOfMealTicketToRender = 0;
	public int numberOfMealTicketToUse = 0;
	public int basedWorkingDays = 0;
	public int totalWorkingTime = 0;

	//I riepiloghi di ogni giorno
	public List<PersonStampingDayRecap> daysRecap = Lists.newArrayList();

	//I riepiloghi codici sul mese
	public Set<StampModificationType> stampModificationTypeSet = Sets.newHashSet();
	public Set<StampType> stampTypeSet = Sets.newHashSet();
	public Map<AbsenceType, Integer> absenceCodeMap = new HashMap<AbsenceType, Integer>();

	//I riepiloghi mensili (uno per ogni contratto attivo nel mese)
	public List<IWrapperContractMonthRecap> contractMonths = Lists.newArrayList();
	
	//Template
	public String month_capitalized;	//FIXME toglierlo e metterlo nel messages
	public int numberOfInOut = 0;

	/**
	 * @param personDayManager
	 * @param personManager
	 * @param yearFactory
	 * @param year
	 * @param month
	 * @param person
	 */
	public PersonStampingRecap(PersonDayManager personDayManager,
			PersonDayDao personDayDao,	MealTicketDao mealTicketDao,
			PersonManager personManager,
			ContractMonthRecapManager contractMonthRecapManager,
			PersonStampingDayRecapFactory stampingDayRecapFactory,
			IWrapperFactory wrapperFactory,
			DateUtility dateUtility,
			int year, int month, Person person) {
		
		this.month = month;
		this.year = year;
		
		if(new YearMonth(year,month).equals( new YearMonth(LocalDate.now()) ))  {
			this.currentMonth = true;
		}

		LocalDate begin = new LocalDate(year, month, 1);
		LocalDate end = begin.dayOfMonth().withMaximumValue();

		List<PersonDay> personDays = personDayDao.getPersonDayInPeriod(person, begin, 
				Optional.fromNullable(end));
		
		this.numberOfInOut = Math.max(MIN_IN_OUT_COLUMN, personDayManager
				.getMaximumCoupleOfStampings(personDays) );

		//Costruzione dati da renderizzare
		
		//Contratti del mese
		List<Contract> monthContracts = wrapperFactory
				.create(person).getMonthContracts(year, month);
		
		this.numberOfMealTicketToUse = personDayManager.numberOfMealTicketToUse(personDays);
		this.numberOfMealTicketToRender = personDayManager.numberOfMealTicketToRender(personDays);
		
		for(Contract contract : monthContracts) {
			Optional<ContractMonthRecap> cmr = wrapperFactory.create(contract)
					.getContractMonthRecap(new YearMonth(year, month));
			
			if (cmr.isPresent()) {
				
				this.contractMonths.add(wrapperFactory.create(cmr.get()));
			}
		}

		//Lista person day contente tutti i giorni fisici del mese
		List<PersonDay> totalPersonDays = personDayManager
				.getTotalPersonDayInMonth(personDays, person, year, month);

		LocalDate today = LocalDate.now();
		//calcolo del valore valid per le stamping del mese (persistere??)
		for(PersonDay pd : totalPersonDays) {
			personDayManager.computeValidStampings(pd);

			PersonStampingDayRecap dayRecap = stampingDayRecapFactory
					.create(pd, this.numberOfInOut, Optional.fromNullable(monthContracts));
			this.daysRecap.add(dayRecap);

			this.totalWorkingTime = this.totalWorkingTime + pd.timeAtWork;


			if(stampingDayRecapFactory.wrapperFactory.create(pd).isFixedTimeAtWork()){
				StampModificationType smt = stampingDayRecapFactory
						.stampTypeManager.getStampMofificationType(
						StampModificationTypeCode.FIXED_WORKINGTIME);

				stampModificationTypeSet.add(smt);
			}

			if(pd.date.equals(today) && !pd.isHoliday && !personDayManager.isAllDayAbsences(pd)){

				StampModificationType smt = stampingDayRecapFactory
						.stampTypeManager.getStampMofificationType(
								StampModificationTypeCode.ACTUAL_TIME_AT_WORK);
				stampModificationTypeSet.add(smt);
			}
			if(pd.stampModificationType!=null && !pd.date.isAfter(today)){

				stampModificationTypeSet.add(pd.stampModificationType);
			}

			for(Stamping stamp : pd.stampings){
				if(stamp.stampType!=null && stamp.stampType.identifier!=null){
					stampTypeSet.add(stamp.stampType);
				}
				if(stamp.markedByAdmin){
					StampModificationType smt = stampingDayRecapFactory
							.stampTypeManager.getStampMofificationType(
									StampModificationTypeCode.MARKED_BY_ADMIN);
					stampModificationTypeSet.add(smt);
				}
				if(stamp.markedByEmployee){
					StampModificationType smt = stampingDayRecapFactory
							.stampTypeManager.getStampMofificationType(
									StampModificationTypeCode.MARKED_BY_EMPLOYEE);
					stampModificationTypeSet.add(smt);
				}
				if(stamp.stampModificationType != null) {
					if(stamp.stampModificationType.code.equals(
							StampModificationTypeCode
							.TO_CONSIDER_TIME_AT_TURN_OF_MIDNIGHT.getCode()) ) {
						stampModificationTypeSet.add(stamp.stampModificationType);
					}
				}
			}
		}

		this.numberOfCompensatoryRestUntilToday = personManager.numberOfCompensatoryRestUntilToday(person, year, month);
		
		this.basedWorkingDays = personManager.basedWorkingDays(personDays);
		this.absenceCodeMap = personManager.getAllAbsenceCodeInMonth(totalPersonDays);

		this.month_capitalized = DateUtility.fromIntToStringMonth(month);

	}
}
