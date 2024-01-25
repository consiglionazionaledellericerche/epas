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
import javax.inject.Inject;
import dao.PersonDao;
import dao.PersonOvertimeDao;
import models.Person;
import models.PersonOvertime;
import play.mvc.Controller;
import play.mvc.With;

@With({Resecure.class})
public class PersonOvertimes extends Controller {
  
  @Inject
  static PersonDao personDao; 
  @Inject
  static PersonOvertimeDao personOvertimeDao;

  public static void addHours(Long personId, int year) {
    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    List<PersonOvertime> personOvertimes = personOvertimeDao.personListInYear(person, year);
    render(personOvertimes);
  }
}
