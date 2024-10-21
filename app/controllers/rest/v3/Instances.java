/*
 * Copyright (C) 2024  Consiglio Nazionale delle Ricerche
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

package controllers.rest.v3;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.GsonBuilder;
import cnr.sync.dto.v3.ConfigurationOfficeDto;
import cnr.sync.dto.v3.ContractTerseDto;
import cnr.sync.dto.v3.OfficeShowTerseDto;
import cnr.sync.dto.v3.PersonConfigurationList;
import cnr.sync.dto.v3.PersonConfigurationShowDto;
import cnr.sync.dto.v3.PersonResidualDto;
import common.security.SecurityRules;
import controllers.Resecure;
import controllers.Resecure.BasicAuth;
import dao.ContractDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import helpers.JsonResponse;
import helpers.rest.RestUtils;
import helpers.rest.RestUtils.HttpMethod;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import manager.recaps.personstamping.PersonStampingRecap;
import manager.recaps.personstamping.PersonStampingRecapFactory;
import models.Configuration;
import models.Contract;
import models.Office;
import models.Person;
import models.PersonConfiguration;
import play.mvc.Controller;
import play.mvc.With;

@Slf4j
@With(Resecure.class)
public class Instances extends Controller {
  
  @Inject
  static OfficeDao officeDao;
  @Inject 
  static SecurityRules rules;
  @Inject
  static GsonBuilder gsonBuilder;
  @Inject
  static Offices offices;
  @Inject
  static ContractDao contractDao;
  @Inject
  static PersonDao personDao;
  @Inject
  static IWrapperFactory wrapperFactory;
  @Inject
  static PersonStampingRecapFactory stampingsRecapFactory;

  @BasicAuth  
  public static void list() {
    RestUtils.checkMethod(request, HttpMethod.GET);
    log.debug("Richiesta la lista delle sede di questa istanza");
    List<Office> offices = officeDao.allEnabledOffices();
    val list = 
        offices.stream().map(o -> OfficeShowTerseDto.build(o)).collect(Collectors.toList());
    renderJSON(gsonBuilder.create().toJson(list));
  }
  
  /**
   * La lista dei contratti dei dipendenti di una sede.
   * @param officeId l'identificativo della sede
   * @param code il codice della sede
   * @param codeId il codeId della sede
   */
  public static void contractList(Long officeId, String code, String codeId) {
    RestUtils.checkMethod(request, HttpMethod.GET);
    val office = offices.getOfficeFromRequest(officeId, code, codeId);
    log.debug("Richiesta la lista dei contratti attuali dei dipendenti di {}", office);
    LocalDate monthBegin = LocalDate.now().withDayOfMonth(1);
    LocalDate monthEnd = monthBegin.dayOfMonth().withMaximumValue();
    List<Person> personList = personDao.list(Optional.absent(),
        Sets.newHashSet(Lists.newArrayList(office)), false, monthBegin, monthEnd, true).list();
    List<Contract> contractList = Lists.newArrayList();
    for (Person p : personList) {
      IWrapperPerson wrPerson = wrapperFactory.create(p);
      Contract c = wrPerson.getCurrentContract().get();
      contractList.add(c);
    }
    val list = contractList.stream().map(c -> ContractTerseDto.build(c)).collect(Collectors.toList());
    renderJSON(gsonBuilder.create().toJson(list));
    
  }
  
  /**
   * La lista dei residui orari dei dipendenti di una sede.
   * @param officeId l'identificativo della sede
   * @param code il codice della sede
   * @param codeId il codeId della sede
   */
  public static void residualList(Long officeId, String code, String codeId) {
    RestUtils.checkMethod(request, HttpMethod.GET);
    val office = offices.getOfficeFromRequest(officeId, code, codeId);
    log.debug("Richiesta la lista dei residui orari attuali dei dipendenti di {}", office);
    val yearMonth = YearMonth.now();
    LocalDate monthBegin = LocalDate.now().withDayOfMonth(1);
    LocalDate monthEnd = monthBegin.dayOfMonth().withMaximumValue();
    List<Person> personList = personDao.list(Optional.absent(),
        Sets.newHashSet(Lists.newArrayList(office)), false, monthBegin, monthEnd, true).list();   
    List<PersonResidualDto> list = Lists.newArrayList();
    for (Person person : personList) {
      PersonStampingRecap psDto = 
          stampingsRecapFactory.create(
              person, yearMonth.getYear(), yearMonth.getMonthOfYear(), true);
      PersonResidualDto dto = new PersonResidualDto();
      dto.person = person;
      dto.residual = psDto.contractMonths.stream().mapToInt(cm -> cm.getValue().getRemainingMinutesLastYear() 
          + cm.getValue().getRemainingMinutesCurrentYear()).sum();
      list.add(dto);
    }
    renderJSON(gsonBuilder.create().toJson(list));
  }
  
  /**
   * La configurazione di una sede.
   * @param officeId l'identificativo della sede
   * @param code il codice della sede
   * @param codeId il codeId della sede
   */
  public static void officeConfiguration(Long officeId, String code, String codeId) {
    RestUtils.checkMethod(request, HttpMethod.GET);
    val office = offices.getOfficeFromRequest(officeId, code, codeId);
    log.debug("Richiesta la lista delle configurazioni della sede {}", office);
    List<Configuration> configurationList = office.getConfigurations();
    val list = configurationList.stream().map(c -> ConfigurationOfficeDto.build(c)).collect(Collectors.toList());
    renderJSON(gsonBuilder.create().toJson(list));
  }
  
  /**
   * La lista delle configurazioni personali dei dipendenti di una sede.
   * @param officeId l'identificativo della sede
   * @param code il codice della sede
   * @param codeId il codeId della sede
   */
  public static void peopleConfiguration(Long officeId, String code, String codeId) {
    RestUtils.checkMethod(request, HttpMethod.GET);
    val office = offices.getOfficeFromRequest(officeId, code, codeId);
    log.debug("Richiesta la lista delle configurazioni dei dipendenti di {}", office);
    LocalDate monthBegin = LocalDate.now().withDayOfMonth(1);
    LocalDate monthEnd = monthBegin.dayOfMonth().withMaximumValue();
    List<Person> personList = personDao.list(Optional.absent(),
        Sets.newHashSet(Lists.newArrayList(office)), false, monthBegin, monthEnd, true).list();
    List<PersonConfigurationList> listOfPersonConfigurationList = Lists.newArrayList();
    PersonConfigurationList pcl = new PersonConfigurationList();
    List<PersonConfigurationShowDto> list = Lists.newArrayList();
    for (Person p : personList) {
      log.debug("Richiesta la lista delle configurazioni di {}", p.getFullname());
      pcl.setNumber(p.getNumber());
      for (PersonConfiguration pc : p.getPersonConfigurations()) {
        list.add(PersonConfigurationShowDto.build(pc));        
      }
      pcl.setList(list);
      listOfPersonConfigurationList.add(pcl);
    }
    renderJSON(gsonBuilder.create().toJson(listOfPersonConfigurationList));
  }
}
