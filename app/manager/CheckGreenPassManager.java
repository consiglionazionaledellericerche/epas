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
import dao.CheckGreenPassDao;
import dao.PersonDao;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
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
  private final CheckGreenPassDao passDao;

  @Inject
  public CheckGreenPassManager(PersonDao personDao, StampingManager stampingManager,
      CheckGreenPassDao passDao) {
    this.personDao = personDao;
    this.stampingManager = stampingManager;
    this.passDao = passDao;
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

    //double number = (list.size() * 25) / 100;
    // conteggio quante persone sorteggiare
    BigDecimal num = new BigDecimal((list.size() * 25) /100);
    num = num.setScale(0, RoundingMode.UP);
    Integer peopleToDraw = num.round(MathContext.DECIMAL64).intValue();
    
    //preparo una mappa con chiave il numero dei sorteggi e valore la lista
    //di persone che hanno subito quel numero di sorteggi
    Map<Long, List<Person>> totalMap = checkedPeople(list);
    
    //metto da parte la lista di quelli meno sorteggiati
    List<Person> lessChecked = lessCheckedPeople(totalMap);
    
    List<Person> peopleDrawn = Lists.newArrayList();    
    
    if (lessChecked.size() < peopleToDraw) {
      // se la lista di quelli meno sorteggiati è di dimensione inferiore alla quantità di 
      // persone da sorteggiare devo armeggiare...
      
      //intanto tra le persone da sorteggiare ci metto quelli della lista
      peopleDrawn.addAll(lessChecked);
      
      //mi metto da parte la lista di quelli più sorteggiati
      List<Person> moreChecked = moreCheckedPeople(totalMap);
      
      Map<Integer, Person> map = Maps.newHashMap();
      int counter = 1;    
      for (Person person : moreChecked) {
        map.put(counter, person);
        counter++;
      }
      
      //guardo quante persone tra quelli più sorteggiati devo andare a prendere per 
      //coprire la quantità di persone sorteggiate totali
      int temp = peopleToDraw - lessChecked.size();
      while (temp > 0) {
        int random = (int) (Math.random() * peopleToDraw + 1);
        Person p = map.get(random);
        if (p != null) {
          peopleDrawn.add(p);
          map.remove(random);
          temp--;
        }
      }
      
    } else {
      //caso facile: prendo tutte le persone dalla lista dei meno sorteggiati e tra questi 
      //scelgo random
      Map<Integer, Person> map = Maps.newHashMap();
      int counter = 1;    
      for (Person person : lessChecked) {
        map.put(counter, person);
        counter++;
      }
      
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
    }
    
    return listDrawn(peopleDrawn);
  }

  private List<CheckGreenPass> listDrawn(List<Person> list) {
    List<CheckGreenPass> checkGreenPassList = Lists.newArrayList();
    for (Person person : list) {
      Optional<CheckGreenPass> obj = passDao.byPersonAndDate(person, LocalDate.now());
      if (!obj.isPresent()) {
        CheckGreenPass gp = new CheckGreenPass();
        gp.person = person;
        gp.checked = false;
        gp.checkDate = LocalDate.now();
        gp.save();
        checkGreenPassList.add(gp);
        log.debug("Salvato checkgreenpass per {}", person.fullName());
      } else {
        checkGreenPassList.add(obj.get());
      }

    }
    return checkGreenPassList;
  }

  private Map<Long, List<Person>> checkedPeople(List<Person> activeInDay) {
    Map<Long, List<Person>> map = Maps.newTreeMap();
    List<Person> list = null;
    for (Person person : activeInDay) {
      long count = passDao.howManyTimesChecked(person);
      list = map.get(count);
      if (list == null) {
        list = Lists.newArrayList();
      }
      list.add(person);
      map.put(count, list);
    }
    
    return map;
  }
  
  private List<Person> lessCheckedPeople(Map<Long, List<Person>> map) {
    Long low = null;
    for (Long key : map.keySet()) {
      if (low == null || key.intValue() < low) {
        low = key;
      }
    }
    return map.get(low);
  }
  
  private List<Person> moreCheckedPeople(Map<Long, List<Person>> map) {
    Long high = null;
    for (Long key : map.keySet()) {
      if (high == null || key.intValue() > high) {
        high = key;
      }
    }
    return map.get(high);
  }
}
