package dao.filter;

import com.google.common.base.Optional;

import com.mysema.query.BooleanBuilder;

import models.query.QPerson;

public class QFilters {

  /**
   * Filtro sul nome del QPerson.
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
