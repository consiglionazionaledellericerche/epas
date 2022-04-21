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

import common.security.SecurityRules;
import dao.CheckGreenPassDao;
import dao.OfficeDao;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import manager.CheckGreenPassManager;
import models.CheckGreenPass;
import models.Office;
import models.Person;
import org.joda.time.LocalDate;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Controller green pass.
 *
 * @author dario
 *
 */
@With({Resecure.class})
public class CheckGreenPasses extends Controller {
  
  @Inject
  static OfficeDao officeDao;
  @Inject
  static SecurityRules rules;
  @Inject
  static CheckGreenPassDao passDao;
  @Inject
  static CheckGreenPassManager manager;
  

  /**
   * Ritorna la lista dei sorteggiati per il check del green pass.
   *
   * @param year l'anno di riferimento
   * @param month il mese di riferimento
   * @param day il giorno di riferimento
   * @param officeId l'identificativo della sede
   */
  public static void dailySituation(final Integer year, final Integer month,
      final Integer day, final Long officeId) {
    
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);
    LocalDate date;
    if (year == null || month == null || day == null) {
      date = LocalDate.now();
    } else {
      date = new LocalDate(year, month, day);
    }
    List<CheckGreenPass> list = passDao.listByDate(date, office);
    render(list, office, date);
  }
  
  /**
   * Genera la form di inserimento di una nuova unità di personale da verificare.
   *
   * @param officeId l'identificativo della sede
   * @param date la data
   */
  public static void addPerson(Long officeId, LocalDate date) {
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);
    List<Person> list = manager.peopleActiveInDate(date, office);
    List<CheckGreenPass> greenPassList = manager.peopleDrawn(office, list, date);
    List<Person> filtered = list.stream().filter(p -> greenPassList.stream()
        .noneMatch(gp -> gp.person.equals(p))).collect(Collectors.toList());    

    render(filtered);
  }
  
  /**
   * Salva la nuova persona di cui controllare il green pass.
   *
   * @param person la persona da controllare
   */
  public static void save(Person person) {
    notFoundIfNull(person);
    rules.checkIfPermitted(person.office);
    CheckGreenPass greenPass = new CheckGreenPass();
    LocalDate date = LocalDate.now();
    greenPass.person = person;
    greenPass.checkDate = date;
    greenPass.checked = false;
    greenPass.save();
    Office office = person.office;
    List<CheckGreenPass> list = passDao.listByDate(date, office);
    render("@dailySituation", list, office, date);
  }
  
  /**
   * Elimina il check di green pass dell'identificativo passato.
   *
   * @param checkGreenPassId l'identificativo di green pass da eliminare
   */
  public static void deletePerson(long checkGreenPassId) {
    CheckGreenPass greenPass = passDao.getById(checkGreenPassId);
    notFoundIfNull(greenPass);
    rules.checkIfPermitted(greenPass.person.office);
    Office office = greenPass.person.office;
    LocalDate date = greenPass.checkDate;
    greenPass.delete();
    List<CheckGreenPass> list = passDao.listByDate(date, office);
    flash.error("Eliminato controllo per %s", greenPass.person.fullName());
    render("@dailySituation", list, office, date);
  }
  
  /**
   * Aggiorna lo stato del checkGreenPass.
   *
   * @param checkGreenPassId l'identificativo del checkGreenPass da aggiornare
   */
  public static void checkPerson(long checkGreenPassId) {
    
    CheckGreenPass greenPass = passDao.getById(checkGreenPassId);
    notFoundIfNull(greenPass);
    rules.checkIfPermitted(greenPass.person.office);
    if (greenPass.checked) {
      greenPass.checked = false;
    } else {
      greenPass.checked = true;
    }
    greenPass.save();
    Office office = greenPass.person.office;
    LocalDate date = greenPass.checkDate;
    List<CheckGreenPass> list = passDao.listByDate(date, office);
    flash.success("Aggiornato il controllo per %s", greenPass.person.fullName());
    render("@dailySituation", list, office, date);
  }
  
  public static void checkGreenPassProcedure(LocalDate date, boolean lastAttempt) {
    manager.checkGreenPassProcedure(date);
    renderText("Procedura completata");
  }
}
