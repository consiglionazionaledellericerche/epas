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
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import controllers.Resecure.NoCheck;
import dao.OfficeDao;
import dao.PersonDao;
import it.cnr.iit.epas.NullStringBinder;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.ConsistencyManager;
import manager.OfficeManager;
import manager.PersonDayManager;
import manager.ldap.LdapService;
import manager.ldap.LdapUser;
import manager.recaps.personstamping.PersonStampingDayRecap;
import manager.recaps.personstamping.PersonStampingDayRecapFactory;
import models.Contract;
import models.Office;
import models.Person;
import models.PersonDay;
import models.Stamping;
import models.Stamping.WayType;
import models.User;
import models.enumerate.StampTypes;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.Minutes;
import play.data.binding.As;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Util;
import play.mvc.With;

/**
 * Controller per la gestione delle timbrature via WEB.
 *
 * @author Cristian Lucchesi
 * @author Dario Tagliaferri
 *
 */
@Slf4j
@With(Resecure.class)
public class Clocks extends Controller {

  @Inject
  static OfficeDao officeDao;
  @Inject
  static OfficeManager officeManager;
  @Inject
  static PersonDao personDao;
  @Inject
  static PersonDayManager personDayManager;
  @Inject
  static PersonStampingDayRecapFactory stampingDayRecapFactory;
  @Inject
  static ConsistencyManager consistencyManager;
  @Inject
  static LdapService ldapService;

  /**
   * Mostra la pagina di inizio della timbratura web.
   */
  @NoCheck
  public static void show() {
    LocalDate data = new LocalDate();

    final List<String> addresses = Lists.newArrayList(Splitter.on(",").trimResults()
        .split(Http.Request.current().remoteAddress));

    Set<Office> offices = officeManager.getOfficesWithAllowedIp(addresses);

    if (offices.isEmpty()) {
      flash.error("Le timbrature web non sono permesse da questo terminale! "
          + "Inserire l'indirizzo ip nella configurazione della propria sede per abilitarlo");
      try {
        Secure.login();
      } catch (Throwable ex) {
        log.warn("Eccezione per la login durante la @Clocks.show", ex);
      }
    }

    List<Person> personList =
        personDao.list(Optional.<String>absent(), offices, false, data, data, true).list();
    
    render(data, personList);
  }

  /**
   * Mostra la pagina di login per la timbratura web.
   *
   * @param person la persona che intende loggarsi
   * @param password la password con cui intende loggarsi
   */
  @NoCheck
  public static void clockLogin(Person person, String password) {

    if (person == null) {
      flash.error("Selezionare una persona dall'elenco del personale.");
      show();
    }

    final User user = person.getUser();

    if (user == null) {
      flash.error("La persona selezionata non dispone di un account valido."
          + " Contattare l'amministratore");
      show();
    }

    checkIpEnabled(person);

    if (Security.authenticate(user.getUsername(), password)) {
      // Mark user as connected
      session.put("username", user.getUsername());
      daySituation();
    } else {
      flash.error("Autenticazione fallita!");
      show();
    }
  }

  /**
   * Effettua il login tramite ldap.
   */
  @NoCheck
  public static void ldapLogin(String username, String password) {
    log.debug("Richiesta autenticazione su Timbrature via WEB con credenziali "
        + "LDAP username={}", username);

    Optional<LdapUser> ldapUser = ldapService.authenticate(username, password);

    if (!ldapUser.isPresent()) {
      log.info("Failed clock login using LDAP for {}", username);
      flash.error("Oops! Username o password sconosciuti");
      show();
    }

    log.debug("clockLdapLogin -> LDAP user = {}", ldapUser.get());

    Person person = Ldap.getPersonByLdapUser(ldapUser.get(), Optional.of("/clocks/show"));
    if (person != null) {
      checkIpEnabled(person);
      daySituation();
    }
  }
  
  @Util
  private static void checkIpEnabled(Person person) {
    final List<String> addresses = Lists.newArrayList(Splitter.on(",").trimResults()
        .split(Http.Request.current().remoteAddress));

    if (!officeManager.getOfficesWithAllowedIp(addresses)
        .contains(person.getCurrentOffice().get())) {

      flash.error("Le timbrature web per la persona indicata non sono abilitate da questo"
          + "terminale! Inserire l'indirizzo ip nella configurazione della propria sede per"
          + " abilitarlo");
      show();
    }
  }

  /**
   * Ritorna la situazione giornaliera della persona loggata.
   */
  public static void daySituation() {
    // Se non e' presente lo user in sessione non posso accedere al metodo per via della resecure,
    // Quindi non dovrebbe mai accadere di avere a questo punto uno user null.
    
    User user = Security.getUser().orNull();
    final List<String> addresses = Lists.newArrayList(Splitter.on(",").trimResults()
        .split(Http.Request.current().remoteAddress));

    if (!officeManager.getOfficesWithAllowedIp(addresses)
        .contains(user.getPerson().getCurrentOffice().get())) {


      flash.error("Le timbrature web per la persona indicata non sono abilitate da questo"
          + "terminale! Inserire l'indirizzo ip nella configurazione della propria sede per"
          + " abilitarlo");
      show();

    }

    final LocalDate today = LocalDate.now();

    final PersonDay personDay = 
        personDayManager.getOrCreateAndPersistPersonDay(user.getPerson(), today);

    int numberOfInOut = personDayManager.numberOfInOutInPersonDay(personDay) + 1;

    PersonStampingDayRecap dayRecap = stampingDayRecapFactory
        .create(personDay, numberOfInOut, true, Optional.<List<Contract>>absent());
    
    render(user, dayRecap, numberOfInOut);

  }

  /**
   * Ritorna la form di inserimento della timbratura.
   *
   * @param wayType verso timbratura.
   */
  public static void webStamping(@Required WayType wayType) {

    if (Validation.hasErrors()) {
      flash.error("E' necessario indicare un verso corretto per la timbratura");
      daySituation();
    }

    final Person currentPerson = Security.getUser().get().getPerson();
    final LocalDate today = LocalDate.now();
    render(wayType, currentPerson, today);
  }

  /**
   * Inserisce la timbratura.
   *
   * @param way       verso timbratura
   * @param stampType Causale timbratura
   * @param note      eventuali note.
   */
  public static void insertWebStamping(WayType way, StampTypes stampType,
      @As(binder = NullStringBinder.class) String note) {

    final User user = Security.getUser().get();
        
    final List<String> addresses = Lists.newArrayList(Splitter.on(",").trimResults()
        .split(Http.Request.current().remoteAddress));

    if (!officeManager.getOfficesWithAllowedIp(addresses)
        .contains(user.getPerson().getCurrentOffice().get())) {

      flash.error("Le timbrature web per la persona indicata non sono abilitate da questo"
          + "terminale! Inserire l'indirizzo ip nella configurazione della propria sede per"
          + " abilitarlo");
      show();
    }

    final PersonDay personDay = personDayManager
        .getOrCreateAndPersistPersonDay(user.getPerson(), LocalDate.now());
    final Stamping stamping = new Stamping(personDay, LocalDateTime.now());

    stamping.setWay(way);
    stamping.setStampType(stampType);
    stamping.setNote(note);

    validation.valid(stamping);

    if (Validation.hasErrors()) {
      flash.error("Timbratura mal formata. Impossibile salvarla");
      daySituation();
    }

    stamping.getPersonDay().getStampings().stream().filter(s -> !stamping.equals(s)).forEach(s -> {

      if (Minutes.minutesBetween(s.getDate(), stamping.getDate()).getMinutes() < 1
          || s.getWay() == stamping.getWay()
          && Minutes.minutesBetween(s.getDate(), stamping.getDate()).getMinutes() < 2) {

        flash.error("Impossibile inserire 2 timbrature così ravvicinate."
            + "Attendere 1 minuto per timbrature nel verso opposto o "
            + "2 minuti per timbrature dello stesso verso");
        daySituation();
      }
    });

    stamping.save();

    consistencyManager.updatePersonSituation(personDay.getPerson().id, personDay.getDate());

    daySituation();
  }
}