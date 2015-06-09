package dao.wrapper.function;

import javax.inject.Inject;

import models.Contract;
import models.ContractMonthRecap;
import models.Office;
import models.Person;
import models.WorkingTimeType;

import com.google.common.base.Function;

import dao.PersonDao;
import dao.PersonLite;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperContractMonthRecap;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperOffice;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.IWrapperWorkingTimeType;

public class WrapperModelFunctionFactory {
	
	private final IWrapperFactory factory;
	private final PersonDao personDao;

	@Inject
	WrapperModelFunctionFactory(IWrapperFactory factory, PersonDao personDao) {
		this.factory = factory;
		this.personDao = personDao;
	}
	
	public Function<WorkingTimeType, IWrapperWorkingTimeType> workingTimeType() {
		return new Function<WorkingTimeType, IWrapperWorkingTimeType>() {

			@Override
			public IWrapperWorkingTimeType apply(WorkingTimeType input) {
				return factory.create(input);
			}
		};
	}

	public Function<Person, IWrapperPerson> person() {
		return new Function<Person, IWrapperPerson>() {

			@Override
			public IWrapperPerson apply(Person input) {
				return factory.create(input);
			}
		};
	}
	
	public Function<PersonLite, IWrapperPerson> personLite() {
		return new Function<PersonLite, IWrapperPerson>() {

			@Override
			public IWrapperPerson apply(PersonLite input) {
				
				return factory.create(personDao.getPersonById(input.id));
			}
		};
	}
	
	public Function<Office, IWrapperOffice> office() {
		return new Function<Office, IWrapperOffice>() {

			@Override
			public IWrapperOffice apply(Office input) {
				return factory.create(input);
			}
		};
	}
	
	public Function<Contract, IWrapperContract> contract() {
		return new Function<Contract, IWrapperContract>() {

			@Override
			public IWrapperContract apply(Contract input) {
				return factory.create(input);
			}
		};
	}
	
	public Function<ContractMonthRecap, IWrapperContractMonthRecap> contractMonthRecap() {
		return new Function<ContractMonthRecap, IWrapperContractMonthRecap>() {

			@Override
			public IWrapperContractMonthRecap apply(ContractMonthRecap input) {
				return factory.create(input);
			}
		};
	}
}

