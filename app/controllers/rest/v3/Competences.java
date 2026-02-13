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

package controllers.rest.v3;

import cnr.sync.dto.v3.CompetenceCodeShowDto;
import cnr.sync.dto.v3.CompetenceCodeShowTerseDto;
import cnr.sync.dto.v3.CompetenceShowDto;
import cnr.sync.dto.v3.PersonCompetenceCodeShowDto;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.GsonBuilder;
import common.security.SecurityRules;
import controllers.Resecure;
import controllers.rest.v2.Persons;
import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import helpers.JodaConverters;
import helpers.JsonResponse;
import helpers.rest.RestUtils;
import helpers.rest.RestUtils.HttpMethod;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import models.Competence;
import models.CompetenceCode;
import models.Contract;
import models.Office;
import models.Person;
import models.PersonCompetenceCodes;
import play.mvc.Controller;
import play.mvc.With;

/**
 * API Rest per l'esportazione delle informazioni sulle competenze
 * e sulla loro assegnazione alle persona.
 *
 * @author Cristian Lucchesi
 *
 */
@With(Resecure.class)
@Slf4j
public class Competences extends Controller {

  @Inject 
  static SecurityRules rules;
  @Inject
  static GsonBuilder gsonBuilder;
  @Inject
  static PersonDao personDao;
  @Inject
  static CompetenceCodeDao competenceCodeDao;
  @Inject
  static CompetenceDao competenceDao;
  @Inject
  static OfficeDao officeDao;
  @Inject
  static IWrapperFactory wrapperFactory;

  /**
   * Metodo rest che ritorna un json contenente la lista dei codici di competenza
   * assegnati ad una persona con le rispettive date di validatà.
   */
  public static void personCompetenceCodes(Long id, String email, String eppn, Long personPerseoId, 
      String fiscalCode, String number, LocalDate date) {
    RestUtils.checkMethod(request, HttpMethod.GET);
    val person = 
        Persons.getPersonFromRequest(id, email, eppn, personPerseoId, fiscalCode, number);
    rules.checkIfPermitted(person.getOffice());
    val pccs = competenceCodeDao.listByPerson(
        person, Optional.fromNullable(JodaConverters.javaToJodaLocalDate(date)));

    renderJSON(gsonBuilder.create().toJson(pccs.stream().map(
        pcc -> PersonCompetenceCodeShowDto.build(pcc)).collect(Collectors.toList())));
  }

  /**
   * Metodo rest che restituisce un json con la lista di tutti i codici di competenza 
   * attivi con alcune informazioni minimali per ogni codice.
   */
  public static void list() {
    RestUtils.checkMethod(request, HttpMethod.GET);
    renderJSON(gsonBuilder.create().toJson(
        competenceCodeDao.getAllCompetenceCode().stream()
          .map(cc -> CompetenceCodeShowTerseDto.build(cc))
          .collect(Collectors.toList())));
  }

  /**
   * Metodo Rest che ritorna il json con tutte le informazioni relative ad una tipologia
   * di competenza.
   */
  public static void show(Long id, String code) {
    RestUtils.checkMethod(request, HttpMethod.GET);
    if (id == null && code == null) {
      JsonResponse.badRequest("E' obbligatorio indicare il parametro id oppure"
          + "il parametro code");
    }
    CompetenceCode competenceCode = null;
    if (id != null) {
      competenceCode = competenceCodeDao.getCompetenceCodeById(id); 
    } else {
      competenceCode = competenceCodeDao.getCompetenceCodeByCode(code);
    }
    RestUtils.checkIfPresent(competenceCode);
    renderJSON(gsonBuilder.create().toJson(CompetenceCodeShowDto.build(competenceCode)));
  }
  
  /**
   * Metodo rest che ritorna la lista delle competenze approvate nell'anno/mese per la persona richiesta.
   * Il mese può essere null e viene ritornata la lista delle competenze approvate nell'anno.
   * 
   * @param personId l'id della persona
   * @param email la mail della persona
   * @param eppn il codice eppn della persona
   * @param personPerseoId l'identificativo dell'anagrafica
   * @param fiscalCode il codice fiscale
   * @param number la matricola
   * @param year l'anno di riferimento
   * @param month il mese di riferimento
   */
  public static void competencesPerPerson(Long personId, String email, String eppn, Long personPerseoId, String fiscalCode, 
      String number, Integer year, Integer month) {
    RestUtils.checkMethod(request, HttpMethod.GET);
    log.debug("Chiamata competencesPerPerson, email = {}, eppn = {}, year = {}, month = {}", email, eppn, year, month);
    val person = 
        Persons.getPersonFromRequest(personId, email, eppn, personPerseoId, fiscalCode, number);
    rules.checkIfPermitted(person.getOffice());
    
    List<PersonCompetenceCodes> pccList = competenceCodeDao
        .listByPerson(person, Optional.fromNullable(org.joda.time.LocalDate.now()
            .withMonthOfYear(month).withYear(year)));
    List<CompetenceCode> codeListIds = Lists.newArrayList();
    for (PersonCompetenceCodes pcc : pccList) {
      codeListIds.add(pcc.getCompetenceCode());
    }
    
    List<Competence> competenceList = competenceDao.getCompetences(Optional.fromNullable(person), 
        year, Optional.fromNullable(month), codeListIds);
    renderJSON(gsonBuilder.create().toJson(competenceList.stream().map(
        comp -> CompetenceShowDto.build(comp)).collect(Collectors.toList())));
    
  }
  
  public static void approvedCompetenceInYear(Integer year, Integer month, boolean onlyDefined, Long officeId) {
    RestUtils.checkMethod(request, HttpMethod.GET);
    log.debug("Chiamata approvedCompetenceInYear, year = {}, month = {}, officeId = {}", year, month, officeId);
    
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    rules.checkIfPermitted(office);
    Set<Person> personSet = Sets.newTreeSet(Person.personComparator());

    Map<Person, Map<CompetenceCode, Integer>> mapPersonCompetenceRecap = 
        Maps.newTreeMap(Person.personComparator());
    List<Competence> competenceInYear = competenceDao
        .getCompetenceInYear(year, Optional.fromNullable(office));
    
    for (Competence competence : competenceInYear) {

      //Filtro tipologia del primo contratto nel mese della competenza
      if (onlyDefined) {
        IWrapperPerson wrPerson = wrapperFactory.create(competence.getPerson());
        Optional<Contract> firstContract = wrPerson
            .getFirstContractInMonth(year, competence.getMonth());
        if (!firstContract.isPresent()) {
          continue;    //questo errore andrebbe segnalato, competenza senza che esista contratto
        }
        IWrapperContract wrContract = wrapperFactory.create(firstContract.get());
        if (!wrContract.isDefined()) {
          continue;    //scarto la competence.
        }
      }
      //Filtro competenza non approvata
      if (competence.getValueApproved() == 0) {
        continue;
      }

      personSet.add(competence.getPerson());

      //aggiungo la competenza alla mappa della persona
      Person person = competence.getPerson();
      Map<CompetenceCode, Integer> personCompetences = mapPersonCompetenceRecap.get(person);

      if (personCompetences == null) {
        personCompetences = Maps.newHashMap();
      }
      Integer value = personCompetences.get(competence.getCompetenceCode());
      if (value != null) {
        value = value + competence.getValueApproved();
      } else {
        value = competence.getValueApproved();
      }

      personCompetences.put(competence.getCompetenceCode(), value);

      mapPersonCompetenceRecap.put(person, personCompetences);

    }
    //renderJSON(gsonBuilder.create().toJson(mapPersonCompetenceRecap.entrySet().stream().map(pcc -> CompetenceShowDto.build(null)))).
  }

}
