package dto;

import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.PersonUtility;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import manager.PersonDayManager;
import manager.PersonManager;
import manager.recaps.PersonResidualMonthRecap;
import manager.recaps.PersonResidualYearRecap;
import models.AbsenceType;
import models.Contract;
import models.Person;
import models.PersonDay;
import models.StampModificationType;
import models.StampType;
import models.rendering.PersonStampingDayRecap;

import com.google.common.collect.Lists;

/**
 * Dto per i template che contengono il tabellone timbrature.
 * 
 * @author alessandro
 *
 */
public class PersonStampingDto {

	private static final int MIN_IN_OUT_COLUMN = 2;
	
	public Person person;
	
	public int year;
	public int month;
	
	public int numberOfInOut = 0;
	
	public int numberOfCompensatoryRestUntilToday = 0;
	
	public int numberOfMealTicketToRender = 0;
	public int numberOfMealTicketToUse = 0;
	
	public int basedWorkingDays = 0;
	
	public List<PersonStampingDayRecap> daysRecap = Lists.newArrayList();
	public List<StampModificationType> stampModificationTypeList = Lists.newArrayList();
	public List<StampType> stampTypeList = Lists.newArrayList();
	
	public Map<AbsenceType, Integer> absenceCodeMap = new HashMap<AbsenceType, Integer>();
	
	public List<PersonResidualMonthRecap> contractMonths = Lists.newArrayList();
	
	public String month_capitalized;

	private PersonStampingDto(){}

	/**
	 * Build del riepilogo per tabellone timbrature.	
	 * @param year
	 * @param month
	 * @param person
	 * @return
	 */
	public static PersonStampingDto build(int year, int month, Person person) {
		
		PersonStampingDto psDto = new PersonStampingDto();
		
		psDto.month = month;
		psDto.year = year;
		
		psDto.numberOfInOut = Math.max(MIN_IN_OUT_COLUMN, PersonUtility.getMaximumCoupleOfStampings(person, year, month));

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
			PersonStampingDayRecap dayRecap = new PersonStampingDayRecap(pd, psDto.numberOfInOut);
			psDto.daysRecap.add(dayRecap);
		}
		psDto.stampModificationTypeList = PersonStampingDayRecap.stampModificationTypeList;
		psDto.stampTypeList = PersonStampingDayRecap.stampTypeList;

		psDto.numberOfCompensatoryRestUntilToday = PersonUtility.numberOfCompensatoryRestUntilToday(person, year, month);
		psDto.numberOfMealTicketToUse = PersonUtility.numberOfMealTicketToUse(person, year, month);
		psDto.numberOfMealTicketToRender = PersonUtility.numberOfMealTicketToRender(person, year, month);
		psDto.basedWorkingDays = PersonUtility.basedWorkingDays(totalPersonDays);
		psDto.absenceCodeMap = PersonUtility.getAllAbsenceCodeInMonth(totalPersonDays);

		List<Contract> monthContracts = PersonManager.getMonthContracts(person,month, year);
		
		for(Contract contract : monthContracts)
		{
			//FIXME factory o AssistedInject per questo Dto
			/*
			PersonResidualYearRecap c = PersonResidualYearRecap.factory(contract, year, null);
			if(c.getMese(month)!=null) {
				psDto.contractMonths.add(c.getMese(month));
			}
			*/
		}

		psDto.month_capitalized = DateUtility.fromIntToStringMonth(month);

		return psDto;
		
	}

}
