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

package manager;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import dao.PersonDao;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.recaps.personstamping.PersonStampingDayRecap;
import models.CheckGreenPass;
import models.Office;
import models.Person;
import org.joda.time.LocalDate;

@Slf4j
public class CheckGreenPassManager {
  
  private final PersonDao personDao;
  private final StampingManager stampingManager;
  
  @Inject
  public CheckGreenPassManager(PersonDao personDao, StampingManager stampingManager) {
    this.personDao = personDao;
    this.stampingManager = stampingManager;
  }
  
  /**
   * Ritorna la lista delle persone attive sulla sede office in data date.
   * @param date la data in cui cercare le persone
   * @param office la sede su cui cercare le persone
   * @return la lista di persone attive nella sede office in data date.
   */
  public List<Person> peopleActiveInDate(LocalDate date, Office office) {
    Set<Office> offices = Sets.newHashSet();
    offices.add(office);
    
    List<Person> list = personDao
        .list(Optional.absent(), offices, false, date, date, true).list();
    int numberOfInOut = stampingManager
        .maxNumberOfStampingsInMonth(date, list);
    List<PersonStampingDayRecap> recapList = stampingManager
        .populatePersonStampingDayRecapList(list, date, numberOfInOut);
    List<Person> filteredList = Lists.newArrayList();
    for (PersonStampingDayRecap recap : recapList) {
      log.debug("Controllo {}", recap.personDay.person.fullName());
      if (!recap.personDay.getStampings().isEmpty()) {
        log.debug("Ci sono timbrature, lo aggiungo alla lista");
        filteredList.add(recap.personDay.person);
      }
    }
    return filteredList;
  }
  
  /**
   * Ritorna la lista di checkGreenPass su persone selezionate.
   * @param list la lista di persone attive
   * @return la lista di checkGreenPass su persone selezionate.
   */
  public List<CheckGreenPass> peopleDrawn(List<Person> list) {
    double number = (list.size() * 25) / 100;
    Integer peopleToDraw = new Integer((int) number);
    int counter = 1;
    Map<Integer, Person> map = Maps.newHashMap();
    for (Person person : list) {
      map.put(counter, person);
      counter++;
    }
    List<Person> peopleDrawn = Lists.newArrayList();
    int temp = peopleToDraw;
    while (temp > 0) {
      int random = (int) (Math.random() * peopleToDraw + 1);
      Person p = map.get(random);
      if (p != null) {
        peopleDrawn.add(p);
        map.remove(random);
        temp--;
      }
    }
    return listDrawn(peopleDrawn);
  }
  
  private List<CheckGreenPass> listDrawn(List<Person> list) {
    List<CheckGreenPass> checkGreenPassList = Lists.newArrayList();
    for (Person person : list) {
      CheckGreenPass gp = new CheckGreenPass();
      gp.person = person;
      gp.checked = false;
      gp.checkDate = LocalDate.now();
      gp.save();
      checkGreenPassList.add(gp);
      log.debug("Salvato checkgreenpass per {}", person.fullName());
    }
    return checkGreenPassList;
  }
}
