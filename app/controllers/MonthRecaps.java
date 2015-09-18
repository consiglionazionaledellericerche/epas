package controllers;

import java.util.List;

import javax.inject.Inject;

import manager.SecureManager;
import models.Contract;
import models.ContractMonthRecap;
import models.Person;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import play.mvc.Controller;
import play.mvc.With;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

import dao.PersonDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.function.WrapperModelFunctionFactory;

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
	
	/**
	 * Controller che gescisce il calcolo del riepilogo annuale residuale delle persone.
	 * 
	 * @param year
	 */
	public static void showRecaps(int year, int month) {

		//FIXME per adesso senza paginazione

		//Prendo la lista delle persone attive in questo momento. 
		//Secondo me si deve mettere le persone non attive in un elenco da poter
		//Analizzare singolarmente.

		List<Person> simplePersonList = personDao.list(
				Optional.<String>absent(),
				secureManager.officesReadAllowed(Security.getUser().get()),
				false, LocalDate.now(), LocalDate.now(), false).list();

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

		render(recaps);
	}

}
