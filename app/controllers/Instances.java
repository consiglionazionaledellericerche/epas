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
package controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import org.apache.commons.lang.WordUtils;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.MonthDay;
import com.google.common.base.Strings;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import cnr.sync.dto.v2.GroupCreateDto;
import cnr.sync.dto.v2.GroupShowTerseDto;
import cnr.sync.dto.v3.BadgeCreateDto;
import cnr.sync.dto.v3.ConfigurationOfficeDto;
import cnr.sync.dto.v3.ContractTerseDto;
import cnr.sync.dto.v3.GroupShowDto;
import cnr.sync.dto.v3.MealTicketResidualDto;
import cnr.sync.dto.v3.OfficeShowTerseDto;
import cnr.sync.dto.v3.PersonAffiliationShowDto;
import cnr.sync.dto.v3.PersonConfigurationList;
import cnr.sync.dto.v3.PersonConfigurationShowDto;
import cnr.sync.dto.v3.PersonResidualDto;
import dao.GroupDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.QualificationDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import helpers.rest.ApiRequestException;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import manager.ConsistencyManager;
import manager.ContractManager;
import manager.PeriodManager;
import manager.PersonManager;
import manager.attestati.dto.show.ListaDipendenti;
import manager.configurations.ConfigurationDto;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import manager.configurations.EpasParam.EpasParamValueType;
import manager.configurations.EpasParam.EpasParamValueType.IpList;
import manager.configurations.EpasParam.EpasParamValueType.LocalTimeInterval;
import manager.configurations.ImportManager;
import manager.recaps.recomputation.RecomputeRecap;
import models.Configuration;
import models.Contract;
import models.Office;
import models.Person;
import models.PersonConfiguration;
import models.absences.GroupAbsenceType;
import models.absences.definitions.DefaultGroup;
import models.base.IPropertyInPeriod;
import models.dto.TeleworkDto;
import models.enumerate.BlockType;
import models.enumerate.ContractType;
import models.flows.Affiliation;
import models.flows.Group;
import play.db.jpa.JPA;
import play.db.jpa.JPAPlugin;
import play.jobs.Job;
import play.libs.WS;
import play.libs.F.Promise;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSRequest;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Util;
import play.mvc.With;

@Slf4j
@With({Resecure.class})
public class Instances extends Controller {

  private static final String JSON_CONTENT_TYPE = "application/json";
  private static final String LIST = "list";
  private static final String RESIDUAL_LIST = "residualList";
  private static final String MEAL_TICKET_RESIDUAL_LIST = "mealTicketResidual";
  private static final String GROUPS = "groups";
  private static final String CONTRACT_LIST = "contractList";
  private static final String OFFICE_CONFIGURATION = "officeConfiguration";
  private static final String PEOPLE_CONFIGURATIONS = "peopleConfiguration";
  private static final String PATH = "/rest/v3/instances";


  @Inject
  static GsonBuilder gsonBuilder;
  @Inject
  static PersonDao personDao;
  @Inject
  static ConfigurationManager configurationManager;
  @Inject
  static PeriodManager periodManager;
  @Inject
  static ConsistencyManager consistencyManager;
  @Inject
  static OfficeDao officeDao;
  @Inject
  static IWrapperFactory wrapperFactory;
  @Inject
  static ContractManager contractManager;
  @Inject
  static PersonManager personManager;
  @Inject
  static QualificationDao qualificationDao;
  @Inject
  static ImportManager importManager;
  @Inject
  static GroupDao groupDao;

  public static void importInstance() {
    render();
  }

  public static void importSeat(String instance) {
    if (Strings.isNullOrEmpty(instance)) {
      flash.error("Inserisci un indirizzo valido");
      importInstance();
    }
    WSRequest wsRequest = WS.url(instance+PATH+"/"+LIST)
        .setHeader("Content-Type", JSON_CONTENT_TYPE)
        .authenticate("developer", "sdrfli.");

    HttpResponse httpResponse = wsRequest.get();
    if (httpResponse.getStatus() == Http.StatusCode.UNAUTHORIZED) {
      log.error("Errore di connessione: {}", httpResponse.getStatusText());
      throw new ApiRequestException("Unauthorized");
    }
    List<OfficeShowTerseDto> list = (List<OfficeShowTerseDto>) new Gson()
        .fromJson(httpResponse.getJson(), new TypeToken<List<OfficeShowTerseDto>>() {}.getRawType());
    render(list, instance);
  }

  public static void importInfo(String instance, String codeId) {
    if (Strings.isNullOrEmpty(instance)) {
      flash.error("Inserisci un indirizzo valido");
      importInstance();
    }
    Office office = null;
    com.google.common.base.Optional<Office> officeInstance = officeDao.byCodeId(codeId);
    if (officeInstance.isPresent()) {
      office = officeInstance.get();
    }
    render(instance, codeId, office);
  }

  public static void importPeopleConfigurations(String instance, String codeId) {
    WSRequest wsRequest = WS.url(instance+PATH+"/"+PEOPLE_CONFIGURATIONS)
        .setHeader("Content-Type", JSON_CONTENT_TYPE)
        .authenticate("developer", "sdrfli.")
        .setParameter("codeId", codeId);

    HttpResponse httpResponse = wsRequest.get();
    if (httpResponse.getStatus() == Http.StatusCode.UNAUTHORIZED) {
      log.error("Errore di connessione: {}", httpResponse.getStatusText());
      throw new ApiRequestException("Unauthorized");
    }
    List<PersonConfigurationList> list = (List<PersonConfigurationList>) new Gson()
        .fromJson(httpResponse.getJson(), new TypeToken<List<PersonConfigurationList>>() {}.getType());
    EpasParam epasParam = null;
    PersonConfiguration newConfiguration = null;
    for (PersonConfigurationList pcl : list) {
      Person person = personDao.getPersonByNumber(pcl.getNumber());
      log.debug("Analizzo la configurazione di {}", person.getFullname());
      for (PersonConfigurationShowDto pcs : pcl.getList()) {
        epasParam = pcs.getEpasParam();
        log.debug("Analizzo il parametro: {} per {}", epasParam.name, person.getFullname());
        Optional<PersonConfiguration> configuration = configurationManager
            .getConfigurationByPersonAndType(person, epasParam);
        if (configuration.isPresent()) {
          newConfiguration = (PersonConfiguration) configurationManager.updateBoolean(epasParam,
              person, Boolean.getBoolean(pcs.getFieldValue()),
              com.google.common.base.Optional.absent(),
              pcs.getEndDate() != null ? com.google.common.base.Optional.fromNullable(LocalDate.parse(pcs.getEndDate())) 
                  : com.google.common.base.Optional.absent(), false);

          List<IPropertyInPeriod> periodRecaps = periodManager
              .updatePeriods(newConfiguration, false);

          RecomputeRecap recomputeRecap =
              periodManager.buildRecap(configuration.get().getPerson().getBeginDate(),
                  com.google.common.base.Optional.fromNullable(LocalDate.now()),
                  periodRecaps, com.google.common.base.Optional.<LocalDate>absent());
          recomputeRecap.epasParam = configuration.get().getEpasParam();
          periodManager.updatePeriods(newConfiguration, true);

          consistencyManager.performRecomputation(configuration.get().getPerson(),
              configuration.get().getEpasParam().recomputationTypes, recomputeRecap.recomputeFrom);
        }
        log.debug("Aggiornato valore del parametro {} per {}", 
            epasParam.name, person.getFullname());
      }
    }
    Office office = officeDao.byCodeId(codeId).get();
    flash.success("Rinnovati i parametri di %s persone", list.size());
    render("@importInfo", instance, codeId, office);
  }


  public static void importOfficeConfiguration(String instance, String codeId) {
    WSRequest wsRequest = WS.url(instance+PATH+"/"+OFFICE_CONFIGURATION)
        .setHeader("Content-Type", JSON_CONTENT_TYPE)
        .authenticate("developer", "sdrfli.")
        .setParameter("codeId", codeId);

    HttpResponse httpResponse = wsRequest.get();
    if (httpResponse.getStatus() == Http.StatusCode.UNAUTHORIZED) {
      log.error("Errore di connessione: {}", httpResponse.getStatusText());
      throw new ApiRequestException("Unauthorized");
    }

    List<ConfigurationOfficeDto> list = (List<ConfigurationOfficeDto>) new Gson()
        .fromJson(httpResponse.getJson(), new TypeToken<List<ConfigurationOfficeDto>>() {}.getType());
    val office = officeDao.byCodeId(codeId).get();

    for (ConfigurationOfficeDto dto : list) {
      importManager.importConfig(dto, office.getId());
    }
    flash.success("Importata configurazione per la sede {}", office.getName());
    render("@importInfo", instance, codeId, office);
  }

  public static void importContracts(String instance, String codeId) {
    WSRequest wsRequest = WS.url(instance+PATH+"/"+CONTRACT_LIST)
        .setHeader("Content-Type", JSON_CONTENT_TYPE)
        .authenticate("developer", "sdrfli.")
        .setParameter("codeId", codeId);

    HttpResponse httpResponse = wsRequest.get();
    if (httpResponse.getStatus() == Http.StatusCode.UNAUTHORIZED) {
      log.error("Errore di connessione: {}", httpResponse.getStatusText());
      throw new ApiRequestException("Unauthorized");
    }
    List<ContractTerseDto> list = (List<ContractTerseDto>) new Gson()
        .fromJson(httpResponse.getJson(), new TypeToken<List<ContractTerseDto>>() {}.getType());
    Person person = null;
    Contract newContract = null;
    int peopleCounter = 0;
    int contractCounter = 0;
    for (ContractTerseDto dto : list) {
      person = personDao.getPersonByNumber(dto.getNumber());
      if (person != null) {
        IWrapperPerson wrPerson = wrapperFactory.create(person);
        com.google.common.base.Optional<Contract> actualContract = wrPerson.getCurrentContract();
        if (actualContract.isPresent()) {
          newContract = new Contract();
          newContract.setBeginDate(LocalDate.parse(dto.getBeginDate()));
          newContract.setEndDate(Strings.isNullOrEmpty(dto.getEndDate()) ? null : LocalDate.parse(dto.getEndDate()));
          newContract.setContractType(ContractType.structured_public_administration);
          newContract.setPerson(person);
          if (actualContract.get().isTemporaryMissing() && actualContract.get().getEndDate() == null) {
            actualContract.get().setEndDate(LocalDate.parse(dto.getBeginDate()).minusDays(1));
            actualContract.get().save();
          }

          boolean esito = contractManager.properContractCreate(newContract, 
              com.google.common.base.Optional.absent(), true);
          if (esito) {
            contractCounter++;
            log.debug("Salvato nuovo contratto per {} con date {} - {}", 
                person.getFullname(), newContract.getBeginDate(), newContract.getEndDate()); 
          } else {
            log.debug("Non c'è stato bisogno di salvare un nuovo contratto per {}", 
                person.getFullname());
          }

        }
      } else {
        log.debug("Creo la nuova persona con matricola {}", dto.getNumber());
        person = new Person();
        person.setName(WordUtils.capitalizeFully(dto.getName()));
        person.setSurname(WordUtils.capitalizeFully(dto.getSurname()));
        person.setEmail(dto.getEmail());
        person.setNumber(dto.getNumber());
        person.setOffice(officeDao.byCodeId(codeId).get());
        person.setQualification(qualificationDao.byQualification(dto.getQualification()).get());
        personManager.properPersonCreate(person);
        person.save();
        newContract = new Contract();
        newContract.setBeginDate(LocalDate.parse(dto.getBeginDate()));
        newContract.setEndDate(Strings.isNullOrEmpty(dto.getEndDate()) ? null : LocalDate.parse(dto.getEndDate()));
        newContract.setContractType(ContractType.structured_public_administration);
        newContract.setPerson(person);
        contractManager.properContractCreate(newContract, 
            com.google.common.base.Optional.absent(), false);
        configurationManager.updateConfigurations(person);

        contractManager.recomputeContract(newContract, 
            com.google.common.base.Optional.<LocalDate>absent(), true, false);
        log.debug("Creata nuova persona {} {}", dto.getName(), dto.getSurname());
        peopleCounter++;
      }

    }        
    flash.success("Inseriti %s nuovi contratti e %s nuove persone", contractCounter, peopleCounter);
    importInfo(instance, codeId);

  }

  public static void importResidual(String instance, String codeId) {
    WSRequest wsRequest = WS.url(instance+PATH+"/"+RESIDUAL_LIST)
        .setHeader("Content-Type", JSON_CONTENT_TYPE)
        .authenticate("developer", "sdrfli.")
        .setParameter("codeId", codeId);

    HttpResponse httpResponse = wsRequest.get();
    if (httpResponse.getStatus() == Http.StatusCode.UNAUTHORIZED) {
      log.error("Errore di connessione: {}", httpResponse.getStatusText());
      throw new ApiRequestException("Unauthorized");
    }
    List<PersonResidualDto> list = (List<PersonResidualDto>) new Gson()
        .fromJson(httpResponse.getJson(), new TypeToken<List<PersonResidualDto>>() {}.getType());
    Person person = null;
    for (PersonResidualDto dto : list) {
      person = personDao.getPersonByNumber(dto.getNumber());
      IWrapperPerson wrPerson = wrapperFactory.create(person);
      com.google.common.base.Optional<Contract> actualContract = wrPerson.getCurrentContract();
      if (actualContract.isPresent()) {
        actualContract.get().setSourceDateResidual(LocalDate.parse(dto.getDate()));
        actualContract.get().setSourceRemainingMinutesCurrentYear(dto.getResidual());
        actualContract.get().setSourceRemainingMinutesLastYear(0);
        actualContract.get().save();
        log.debug("Salvato residuo di {} minuti per {}",dto.getResidual(), person.getFullname());
      }
    }
    Office office = officeDao.byCodeId(codeId).get();
    flash.success("Importati i residui orari di %s persone", list.size());
    render("@importInfo", instance, codeId, office);
  }

  public static void importMealTicketResidual(String instance, String codeId) {
    WSRequest wsRequest = WS.url(instance+PATH+"/"+MEAL_TICKET_RESIDUAL_LIST)
        .setHeader("Content-Type", JSON_CONTENT_TYPE)
        .authenticate("developer", "sdrfli.")
        .setParameter("codeId", codeId);

    HttpResponse httpResponse = wsRequest.get();
    if (httpResponse.getStatus() == Http.StatusCode.UNAUTHORIZED) {
      log.error("Errore di connessione: {}", httpResponse.getStatusText());
      throw new ApiRequestException("Unauthorized");
    }
    List<MealTicketResidualDto> list = (List<MealTicketResidualDto>) new Gson()
        .fromJson(httpResponse.getJson(), new TypeToken<List<MealTicketResidualDto>>() {}.getType());
    Person person = null;
    for (MealTicketResidualDto dto : list) {
      person = personDao.getPersonByNumber(dto.getNumber());
      IWrapperPerson wrPerson = wrapperFactory.create(person);
      com.google.common.base.Optional<Contract> actualContract = wrPerson.getCurrentContract();
      if (actualContract.isPresent()) {
        actualContract.get().setSourceDateMealTicket(LocalDate.parse(dto.getDateOfResidual()));
        actualContract.get().setSourceRemainingMealTicket(dto.getMealTicketResidual());
        actualContract.get().save();
        log.debug("Salvato residuo di {} buoni pasto per {}",
            dto.getMealTicketResidual(), person.getFullname());
      }
    }
    Office office = officeDao.byCodeId(codeId).get();
    flash.success("Importati i buoni residui di %s persone", list.size());
    render("@importInfo", instance, codeId, office);
  }

  public static void importGroups(String instance, String codeId) {
    WSRequest wsRequest = WS.url(instance+PATH+"/"+GROUPS)
        .setHeader("Content-Type", JSON_CONTENT_TYPE)
        .authenticate("developer", "sdrfli.")
        .setParameter("codeId", codeId);

    HttpResponse httpResponse = wsRequest.get();
    if (httpResponse.getStatus() == Http.StatusCode.UNAUTHORIZED) {
      log.error("Errore di connessione: {}", httpResponse.getStatusText());
      throw new ApiRequestException("Unauthorized");
    }
    Office office = officeDao.byCodeId(codeId).get();
    List<GroupShowDto> list = (List<GroupShowDto>) new Gson()
        .fromJson(httpResponse.getJson(), new TypeToken<List<GroupShowDto>>() {}.getType());
    Group group = null;
    for (GroupShowDto dto : list) {
      if (groupDao.checkGroupByOfficeAndName(office, dto.getName()).isPresent()) {
        log.debug("Gruppo {} già esistente, procedo solo con le affiliazioni", dto.getName());
        group = groupDao.checkGroupByOfficeAndName(office, dto.getName()).get();
      } else {
        group = new Group();
        group.setDescription(dto.getDescription());
        group.setEndDate(Strings.isNullOrEmpty(dto.getEndDate()) ? null : java.time.LocalDate.parse(dto.getEndDate()));
        group.setName(dto.getName());
        group.setOffice(officeDao.byCodeId(codeId).get());
        group.setManager(personDao.getPersonByNumber(dto.getManager()));
        group.save();
      }      
      log.debug("Inizio a costruire le affiliazioni");
      for (PersonAffiliationShowDto pasDto : dto.getList()) {   
        Person person = personDao.getPersonByNumber(pasDto.getNumber());
        if (person == null) {
          continue;
        }
        log.debug("Creo affiliazione per {}", person.getFullname());
        Affiliation affiliation = new Affiliation();
        affiliation.setPerson(personDao.getPersonByNumber(pasDto.getNumber()));
        affiliation.setBeginDate(java.time.LocalDate.parse(pasDto.getBeginDate()));
        affiliation.setEndDate(!Strings.isNullOrEmpty(pasDto.getEndDate()) ? 
            java.time.LocalDate.parse(pasDto.getEndDate()) : null);
        affiliation.setGroup(group);
        affiliation.save();
        log.debug("Salvata affiliazione di {} al gruppo {}", person.getFullname(), group.getName());
      }
      log.debug("Inserito gruppo {} con manager {}", group.getName(), group.getManager().getFullname());
    }
    
    flash.success("Importati %s gruppi", list.size());
    render("@importInfo", instance, codeId, office);
  }
  
  
}
