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

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import common.security.SecurityRules;
import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.absences.AbsenceComponentDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.function.WrapperModelFunctionFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.Data;
import manager.MonthsRecapManager;
import manager.PersonManager;
import manager.SecureManager;
import manager.services.absences.AbsenceService;
import manager.services.absences.model.VacationSituation;
import models.Contract;
import models.ContractMonthRecap;
import models.Office;
import models.Person;
import models.PersonDay;
import models.absences.GroupAbsenceType;
import models.absences.definitions.DefaultGroup;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Controller per la gestione dei MonthRecap.
 */
@With({Resecure.class})
public class MonthRecaps extends Controller {

  @Inject
  private static SecureManager secureManager;
  @Inject
  private static PersonDao personDao;
  @Inject
  private static WrapperModelFunctionFactory wrapperFunctionFactory;
  @Inject
  private static IWrapperFactory wrapperFactory;
  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static PersonManager personManager;
  @Inject
  private static SecurityRules rules;
  @Inject
  private static AbsenceService absenceService;
  @Inject
  private static AbsenceComponentDao absenceComponentDao;
  @Inject
  private static PersonDayDao personDayDao;
  @Inject
  private static MonthsRecapManager monthsRecapManager;

  /**
   * Controller che gescisce il calcolo del riepilogo annuale residuale delle persone.
   */
  public static void showRecaps(int year, int month, Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);

    LocalDate monthBegin = new LocalDate(year, month, 1);
    LocalDate monthEnd = new LocalDate(year, month, 1).dayOfMonth().withMaximumValue();

    List<Person> simplePersonList = personDao.list(
        Optional.<String>absent(), Sets.newHashSet(office),
        false, monthBegin, monthEnd, false).list();

    List<IWrapperPerson> personList = FluentIterable
        .from(simplePersonList)
        .transform(wrapperFunctionFactory.person()).toList();

    List<ContractMonthRecap> recaps = Lists.newArrayList();

    for (IWrapperPerson person : personList) {

      for (Contract c : person.getValue().getContracts()) {
        IWrapperContract contract = wrapperFactory.create(c);

        YearMonth yearMonth = new YearMonth(year, month);

        Optional<ContractMonthRecap> recap = contract.getContractMonthRecap(yearMonth);
        if (recap.isPresent()) {
          recaps.add(recap.get());
        } else {
          //System.out.println(person.getValue().fullName());
        }
      }
    }


    render(recaps, year, month, office);
  }
  
  /**
   * Genera il file con le info su smart working / lavoro in sede.
   *
   * @param month il mese di riferimento
   * @param year l'anno di riferimento
   * @param officeId l'id della sede
   */
  public static void generateSmartWorkingMonthlyRecap(int month, int year, Long officeId) {
    
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);
    
    Map<Person, List<PersonDay>> map = Maps.newHashMap();
    YearMonth yearMonth = new YearMonth(year, month);
    Set<Office> offices = Sets.newHashSet();
    offices.add(office);
    List<Person> simplePersonList = personDao.getActivePersonInMonth(offices, yearMonth);
        
    for (Person person : simplePersonList) {
      map.put(person, personDayDao.getPersonDayInMonth(person, yearMonth));
    }
    InputStream file = null;
    try {
      file = monthsRecapManager.buildFile(yearMonth, simplePersonList, office);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    renderBinary(file);
  }

  /**
   * Recap chiesto da IVV. TODO: una raccolta di piccole funzionalità
   *
   * @param year     anno
   * @param month    mese
   * @param officeId sede
   */
  public static void customRecap(final int year, final int month, Long officeId) {

    Set<Office> offices = secureManager
        .officesReadAllowed(Security.getUser().get());
    if (offices.isEmpty()) {
      forbidden();
    }
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);

    LocalDate monthBegin = new LocalDate().withYear(year)
        .withMonthOfYear(month).withDayOfMonth(1);
    LocalDate monthEnd = new LocalDate().withYear(year)
        .withMonthOfYear(month).dayOfMonth().withMaximumValue();

    List<Person> activePersons = personDao.list(Optional.<String>absent(),
        Sets.newHashSet(office), false, monthBegin, monthEnd, true)
        .list();

    List<CustomRecapDto> customRecapList = Lists.newArrayList();

    GroupAbsenceType vacationGroup = absenceComponentDao
        .groupAbsenceTypeByName(DefaultGroup.FERIE_CNR.name()).get();
    
    for (Person person : activePersons) {

      IWrapperPerson wrPerson = wrapperFactory.create(person);

      for (Contract contract : wrPerson.orderedMonthContracts(year, month)) {

        VacationSituation situation = absenceService
            .buildVacationSituation(contract, year, vacationGroup, Optional.of(monthEnd), false);

        // Danila voleva un riepilogo con data residuale la fine del mese.
        // Per essere danila compliant andrebbero tolte dal conteggio le assenze effettuate 
        // dopo monthEnd. Anche chissene.

        CustomRecapDto danilaDto = new CustomRecapDto();
        danilaDto.ferieAnnoCorrente = situation.currentYear.usableTotal();

        danilaDto.ferieAnnoPassato = situation.lastYear != null 
            ? situation.lastYear.usableTotal() : 0;

        danilaDto.permessi = situation.permissions.usableTotal();

        Optional<ContractMonthRecap> recap =
            wrapperFactory.create(contract).getContractMonthRecap(
                new YearMonth(year, month));

        danilaDto.monteOreAnnoPassato = recap.get()
            .getRemainingMinutesLastYear();
        danilaDto.monteOreAnnoCorrente = recap.get()
            .getRemainingMinutesCurrentYear();
        danilaDto.giorni = 22 - personManager
            .numberOfCompensatoryRestUntilToday(person,
                year, month);

        danilaDto.straordinariFeriali = recap.get()
            .getStraordinariMinutiS1Print();
        danilaDto.straordinariFestivi = recap.get()
            .getStraordinariMinutiS2Print();
        danilaDto.person = person;
        customRecapList.add(danilaDto);
      }

    }
    render(customRecapList, offices, office, year, month);
  }

  /**
   *  Raccoglitore per il CustomRecap.
   *
   *  @author Alessandro Martelli
   */
  @Data
  public static class CustomRecapDto {
    public Person person;
    public int ferieAnnoPassato;
    public int ferieAnnoCorrente;
    public int permessi;
    public int monteOreAnnoPassato;
    public int monteOreAnnoCorrente;
    public int giorni;
    public int straordinariFeriali;
    public int straordinariFestivi;
  }

}
