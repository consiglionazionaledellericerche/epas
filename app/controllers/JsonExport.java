/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package controllers;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import dao.OfficeDao;
import dao.PersonDao;
import java.util.HashSet;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import models.Office;
import models.Person;
import org.joda.time.LocalDate;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Classe che permette l'esportazione di persone e sedi.
 * 
 */
@Slf4j
@With(Resecure.class)
public class JsonExport extends Controller {

  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static PersonDao personDao;

  // TODO per il momento il ruolo developer è l'unico a poter utilizzare questo metodo
  /**
   * Ritorna il json contenente la lista delle persone attive.
   */
  public static void activePersons() {

    List<Office> offices = officeDao.getAllOffices();
    List<Person> activePersons = personDao.list(Optional.<String>absent(),
            new HashSet<Office>(offices), false, LocalDate.now(), LocalDate.now(), true).list();
    log.debug("activePersons.size() = %d", activePersons.size());

    List<PersonInfo> activePersonInfos =
        FluentIterable.from(activePersons).transform(
            new Function<Person, PersonInfo>() {
              @Override
              public PersonInfo apply(Person person) {
                return new PersonInfo(
                Joiner.on(" ").skipNulls().join(person.getName(), person.getOthersSurnames()),
                Joiner.on(" ").skipNulls().join(person.getSurname(), person.getOthersSurnames()),
                person.getUser().getPassword());
              }
            }
       ).toList();

    renderJSON(activePersonInfos);
  }

  static final class PersonInfo {
    private final String nome;
    private final String cognome;
    private final String password;

    public PersonInfo(String nome, String cognome, String password) {
      this.nome = nome;
      this.cognome = cognome;
      this.password = password;
    }

    public String getNome() {
      return nome;
    }

    public String getCognome() {
      return cognome;
    }

    public String getPassword() {
      return password;
    }

  }

}
