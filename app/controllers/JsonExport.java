/**
 *
 */
package controllers;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;

import dao.OfficeDao;
import dao.PersonDao;

import models.Office;
import models.Person;

import org.joda.time.LocalDate;

import play.Logger;
import play.mvc.Controller;
import play.mvc.With;

import java.util.HashSet;
import java.util.List;

import javax.inject.Inject;

/**
 * @author cristian
 */
@With(Resecure.class)
public class JsonExport extends Controller {

  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static PersonDao personDao;

  // TODO per il momento il ruolo developer è l'unico a poter utilizzare questo metodo
  public static void activePersons() {

    List<Office> offices = officeDao.getAllOffices();
    List<Person> activePersons = personDao.list(Optional.<String>absent(),
            new HashSet<Office>(offices), false, LocalDate.now(), LocalDate.now(), true).list();
    Logger.debug("activePersons.size() = %d", activePersons.size());

    List<PersonInfo> activePersonInfos = FluentIterable.from(activePersons).transform(new Function<Person, PersonInfo>() {
      @Override
      public PersonInfo apply(Person person) {
        return new PersonInfo(
                Joiner.on(" ").skipNulls().join(person.name, person.othersSurnames),
                Joiner.on(" ").skipNulls().join(person.surname, person.othersSurnames),
                person.user.password);
      }
    }).toList();

    renderJSON(activePersonInfos);
  }

  final static class PersonInfo {
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
