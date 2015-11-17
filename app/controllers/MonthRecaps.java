package controllers;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

import dao.OfficeDao;
import dao.PersonDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.function.WrapperModelFunctionFactory;
import manager.SecureManager;
import models.Contract;
import models.ContractMonthRecap;
import models.Office;
import models.Person;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import javax.inject.Inject;

import java.util.List;
import java.util.Set;

@With( {Resecure.class, RequestInit.class} )
public class MonthRecaps extends Controller{

	@Inject
	private static SecureManager secureManager;
	@Inject
	private static PersonDao personDao;
	@Inject
	private static WrapperModelFunctionFactory wrapperFunctionFactory;
	@Inject
	private static IWrapperFactory wrapperFactory;
	@Inject
	private static OfficeDao officeDao;
	@Inject
	private static SecurityRules rules;
	
	/**
	 * Controller che gescisce il calcolo del riepilogo annuale residuale delle persone.
	 * 
	 * @param year
	 */
	public static void showRecaps(int year, int month, Long officeId) {
		
		Set<Office> offices = secureManager
				.officesReadAllowed(Security.getUser().get());
		if (offices.isEmpty()) {
			forbidden();
		}
		Office office = officeDao.getOfficeById(officeId);
		notFoundIfNull(office);
		rules.checkIfPermitted(office);
		
		LocalDate monthBegin = new LocalDate(year, month, 1);
		LocalDate monthEnd = new LocalDate(year, month, 1).dayOfMonth().withMaximumValue();
		
		List<Person> simplePersonList = personDao.list(
				Optional.<String>absent(),
				secureManager.officesReadAllowed(Security.getUser().get()),
				false, monthBegin, monthEnd, false).list();

		List<IWrapperPerson> personList = FluentIterable
				.from(simplePersonList)
				.transform(wrapperFunctionFactory.person()).toList();

		List<ContractMonthRecap> recaps = Lists.newArrayList();
		
		for(IWrapperPerson person : personList) {
			
			for(Contract c : person.getValue().contracts) {
				IWrapperContract contract = wrapperFactory.create(c);
			
				YearMonth yearMonth = new YearMonth(year, month); 
			
				Optional<ContractMonthRecap> recap = contract.getContractMonthRecap( yearMonth );
				if (recap.isPresent()) {
					recaps.add(recap.get());
				} else { 
					//System.out.println(person.getValue().fullName());
				}
			}
		}

		render(recaps, year, month);
	}

}
