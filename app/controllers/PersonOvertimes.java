/*
 * Copyright (C) 2023  Consiglio Nazionale delle Ricerche
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

import java.util.List;
import java.util.regex.Pattern;
import javax.inject.Inject;
import common.security.SecurityRules;
import dao.PersonDao;
import dao.PersonOvertimeDao;
import helpers.Web;
import lombok.extern.slf4j.Slf4j;
import models.Person;
import models.PersonOvertime;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;

@With({Resecure.class})
@Slf4j
public class PersonOvertimes extends Controller {
  
  @Inject
  static PersonDao personDao; 
  @Inject
  static PersonOvertimeDao personOvertimeDao;
  @Inject
  static SecurityRules rules;

  public static void addHours(Long personId, int year) {
    
    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    rules.checkIfPermitted(person.getOffice());
    List<PersonOvertime> personOvertimes = personOvertimeDao.personListInYear(person, year);
    PersonOvertime personOvertime = new PersonOvertime();
    render(personOvertimes, person, year, personOvertime);
  }
  
  public static void saveHours(PersonOvertime personOvertime, 
      int year, Long personId) {
    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    rules.checkIfPermitted(person.getOffice());
    Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");
    
    if (personOvertime.getNumberOfHours() == null 
        || !pattern.matcher(personOvertime.getNumberOfHours().toString()).matches()) {
      Validation.addError("personOvertime.getNumberOfHours", "Inserire una quantità numerica!");
    }
    if (personOvertime.getDateOfUpdate() == null) {
      Validation.addError("personOvertime.dateOfUpdate", "Inserire una data valida!!");
    }
    if (personOvertime.getDateOfUpdate().getYear() != year) {
      Validation.addError("personOvertime.dateOfUpdate", 
          "Si sta inserendo una quantità per un anno diverso da quello specificato nella data!!");
    }
    if (Validation.hasErrors()) {
      response.status = 400;
      render("@addHours", person, year, personOvertime);
    }
    personOvertime.setPerson(person);
    personOvertime.setYear(year);
    personOvertime.save();
    flash.success("Aggiunta nuova quantità al monte ore per straordinari di %s", 
        person.getFullname());
    Competences.totalOvertimeHours(year, person.getOffice().id);
    
  }
}
