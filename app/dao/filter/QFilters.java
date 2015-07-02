package dao.filter;

import models.query.QPerson;

import com.google.common.base.Optional;
import com.mysema.query.BooleanBuilder;

public class QFilters {

	/**
	 * Filtro sul nome del QPerson.
	 * 
	 * @param condition
	 * @param name 
 	 */
	public BooleanBuilder filterNameFromPerson(QPerson person, Optional<String> name) {
		
		BooleanBuilder condition = new BooleanBuilder();
		
		if (name.isPresent() && !name.get().trim().isEmpty()) {
			condition.andAnyOf(person.name.startsWithIgnoreCase(name.get()),
					person.surname.startsWithIgnoreCase(name.get()));
		}
		
		return condition;
	}
}
