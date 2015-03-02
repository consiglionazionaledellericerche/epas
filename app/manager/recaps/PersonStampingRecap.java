package manager.recaps;

import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.PersonUtility;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import manager.PersonDayManager;
import manager.PersonManager;
import models.AbsenceType;
import models.Contract;
import models.Person;
import models.PersonDay;
import models.StampModificationType;
import models.StampType;
import models.rendering.PersonStampingDayRecap;

import com.google.common.collect.Lists;

public class PersonStampingRecap {

	private static final int MIN_IN_OUT_COLUMN = 2;

	private final PersonDayManager personDayManager;
	private final PersonManager personManager;
	private final PersonResidualYearRecapFactory yearFactory;
	
	public Person person;
	public int year;
	public int month;

	//Informazioni sul mese
	public int numberOfCompensatoryRestUntilToday = 0;
	public int numberOfMealTicketToRender = 0;
	public int numberOfMealTicketToUse = 0;
	public int basedWorkingDays = 0;
	
	//I riepiloghi di ogni giorno
	public List<PersonStampingDayRecap> daysRecap = Lists.newArrayList();
	
	//I riepiloghi codici sul mese
	public List<StampModificationType> stampModificationTypeList = Lists.newArrayList();
	public List<StampType> stampTypeList = Lists.newArrayList();
	public Map<AbsenceType, Integer> absenceCodeMap = new HashMap<AbsenceType, Integer>();

	//I riepiloghi mensili (uno per ogni contratto attivo nel mese)
	public List<PersonResidualMonthRecap> contractMonths = Lists.newArrayList();
	
	//Template
	public String month_capitalized;	//FIXME toglierlo e metterlo nel messages
	public int numberOfInOut = 0;

	/**
	 * Costruisce il riepilogo mensile di una persona. 
	 * Alimenta la vista tabellone timbrature.
	 * @param personDayManager
	 * @param personManager
	 * @param yearFactory
	 * @param year
	 * @param month
	 * @param person
	 */
	public PersonStampingRecap(PersonDayManager personDayManager,
			PersonManager personManager,
			PersonResidualYearRecapFactory yearFactory,
			
			int year, int month, Person person) {
		
		this.personDayManager = personDayManager;
		this.personManager = personManager;
		this.yearFactory = yearFactory;
		
		this.month = month;
		this.year = year;
		
		this.numberOfInOut = Math.max(MIN_IN_OUT_COLUMN, PersonUtility.getMaximumCoupleOfStampings(person, year, month));

		//Costruzione dati da renderizzare
		
		//Lista person day contente tutti i giorni fisici del mese
		List<PersonDay> totalPersonDays = PersonUtility.getTotalPersonDayInMonth(person, year, month);
		
		//calcolo del valore valid per le stamping del mese (persistere??)
		for(PersonDay pd : totalPersonDays) {
			PersonDayManager.computeValidStampings(pd);
		}
		
		PersonStampingDayRecap.stampModificationTypeList = Lists.newArrayList(); 
		PersonStampingDayRecap.stampTypeList = Lists.newArrayList();
		for(PersonDay pd : totalPersonDays ) {
			PersonStampingDayRecap dayRecap = new PersonStampingDayRecap(pd, this.numberOfInOut);
			this.daysRecap.add(dayRecap);
		}
		this.stampModificationTypeList = PersonStampingDayRecap.stampModificationTypeList;
		this.stampTypeList = PersonStampingDayRecap.stampTypeList;

		this.numberOfCompensatoryRestUntilToday = PersonUtility.numberOfCompensatoryRestUntilToday(person, year, month);
		this.numberOfMealTicketToUse = PersonUtility.numberOfMealTicketToUse(person, year, month);
		this.numberOfMealTicketToRender = PersonUtility.numberOfMealTicketToRender(person, year, month);
		this.basedWorkingDays = PersonUtility.basedWorkingDays(totalPersonDays);
		this.absenceCodeMap = PersonUtility.getAllAbsenceCodeInMonth(totalPersonDays);

		List<Contract> monthContracts = this.personManager.getMonthContracts(person,month, year);
		
		for(Contract contract : monthContracts)
		{
			PersonResidualYearRecap c = this.yearFactory.create(contract, year, null);
			if(c.getMese(month)!=null) {
				this.contractMonths.add(c.getMese(month));
			}
		}

		this.month_capitalized = DateUtility.fromIntToStringMonth(month);
		
	}
	
}
