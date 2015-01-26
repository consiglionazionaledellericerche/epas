package dao.wrapper;

import com.google.common.base.Function;
import com.google.inject.Inject;

import models.Person;

/**
 * @author marco
 *
 */
public final class WrapperFunction implements Function<Person, WrapperPerson>{

	private final IWrapperFactory factory;

	@Inject
	WrapperFunction(IWrapperFactory factory) {
		this.factory = factory;
	}

	@Override
	public WrapperPerson apply(Person person) {
		return factory.create(person);
	}

	// TODO: aggiungere le funzioni di creazione dei wrapper.
}
