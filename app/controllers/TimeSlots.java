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

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import common.security.SecurityRules;
import dao.OfficeDao;
import dao.TimeSlotDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperTimeSlot;
import dao.wrapper.function.WrapperModelFunctionFactory;
import helpers.Web;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import models.Contract;
import models.ContractMandatoryTimeSlot;
import models.Office;
import models.TimeSlot;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Controller per la gestione dei TimeSlot.
 */
@Slf4j
@With(Resecure.class)
public class TimeSlots extends Controller {

  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static TimeSlotDao timeSlotDao;
  @Inject
  private static SecurityRules rules;
  @Inject
  private static WrapperModelFunctionFactory wrapperFunctionFactory;
  @Inject
  private static IWrapperFactory wrapperFactory;

  /**
   * Permette la gestione delle fasce orarie appartenenti alla sede con identificativo
   * officeId.
   *
   * @param officeId l'identificativo della sede
   */
  public static void manageTimeSlots(Long officeId) {
    
    if (officeId == null) {
      officeId = Long.parseLong(session.get("officeSelected"));
    }
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);
    
    val timeSlots = FluentIterable
        .from(timeSlotDao.getPredefinedEnabledTimeSlots())
        .transform(wrapperFunctionFactory.timeSlot()).toList();
    render(timeSlots, office);
  }
  
  /**
   * Interfaccia di gestione delle fasce orarie associate da un ufficio.
   *
   * @param officeId l'identificativo della sede
   */
  public static void manageOfficeTimeSlots(Long officeId) {
    
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);
    
    List<IWrapperTimeSlot> tsAllowed = FluentIterable
        .from(office.timeSlots)
        .transform(wrapperFunctionFactory.timeSlot()).toList();

    List<IWrapperTimeSlot> tsAllowedEnabled = Lists.newArrayList();
    List<IWrapperTimeSlot> tsAllowedDisabled = Lists.newArrayList();
    for (IWrapperTimeSlot ts : tsAllowed) {
      if (ts.getValue().disabled) {
        tsAllowedDisabled.add(ts);
      } else {
        tsAllowedEnabled.add(ts);
      }
    }
    render(office, tsAllowedEnabled, tsAllowedDisabled);
  }
  
  public static void blank(TimeSlot timeSlot, Long officeId) {
    render(timeSlot, officeId);
  }
  
  /**
   * Salvataggio delle fascie oraria obbligatorie.
   *
   * @param timeSlot la fascia oraria da salvare
   */
  public static void save(@Valid TimeSlot timeSlot) {
    notFoundIfNull(timeSlot);

    if (timeSlot.office != null) {
      rules.checkIfPermitted(timeSlot.office);
    } else {
      rules.checkAction("TimeSlots.savePredefined");
    }
    if (Validation.hasErrors()) {
      response.status = 400;
      log.warn("validation errors for {}: {}", timeSlot,
          validation.errorsMap());
      Long officeId = timeSlot.office != null ? timeSlot.office.id : null;
      render("@blank", timeSlot, officeId);
    } else {
      timeSlot.save();
      log.info("Creata nuova fascia di orario {}, office = {}", 
          timeSlot.getLabel(), timeSlot.office);
    }
    manageTimeSlots(timeSlot.office != null ? timeSlot.office.id : null);
  }
  
  /**
   * I contratti attivi che per quella sede hanno quel tipo orario.
   *
   * @param tsId    orario
   * @param officeId sede
   */
  public static void showContracts(Long tsId, Long officeId) {

    val optionalTs = timeSlotDao.byId(tsId);
    notFoundIfNull(optionalTs.orNull());
    val ts = optionalTs.get();
    
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    rules.checkIfPermitted(ts.office);
    rules.checkIfPermitted(office);

    List<Contract> contractList = wrapperFactory.create(ts).getAssociatedActiveContract(office);

    render(ts, contractList, office);

  }

  /**
   * Mostra i periodi con quella fascia di orario di lavoro appartenenti a contratti
   * attualmente attivi.
   *
   * @param tsId id della fascia di lavoro
   * @param officeId sede
   */
  public static void showContractMandatoryTimeSlot(Long tsId, Long officeId) {

    val optionalTs = timeSlotDao.byId(tsId);
    notFoundIfNull(optionalTs.orNull());    
    val ts = optionalTs.get();

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    rules.checkIfPermitted(ts.office);
    rules.checkIfPermitted(office);

    IWrapperTimeSlot wts = wrapperFactory.create(ts);

    List<ContractMandatoryTimeSlot> cmtsList = wts.getAssociatedPeriodInActiveContract(office);

    render(ts, cmtsList, office);

  }
  
  /**
   * Cancellazione di una fascia oraria.
   *
   * @param id l'identificativo della fascia oraria da cancellare
   */
  public static void delete(Long id) {
    notFoundIfNull(id);
    val timeSlot = timeSlotDao.byId(id).orNull();
    notFoundIfNull(timeSlot);
    
    if (timeSlot.office != null) {
      rules.checkIfPermitted(timeSlot.office);
    } else {
      rules.checkAction("TimeSlots.deletePredefined");
    }
    
    //elimino la sorgente se non è associata ad alcun gruppo.
    if (timeSlot.contractMandatoryTimeSlots.isEmpty()) {
      timeSlot.delete();
      flash.success(Web.msgDeleted(TimeSlot.class));
    } else {
      flash.error("Per poter una fascia oraria è necessario che non sia associata ad alcun "
          + "contratto.");      
    }
    if (timeSlot.office != null) {
      manageOfficeTimeSlots(timeSlot.office.id);
    } else {
      manageTimeSlots(null);  
    }
  }

  /**
   * Abilita/Disabilita un fascia oraria.
   *
   * @param id l'identificativo della fascia oraria da abilitare/disabilitare
   */
  public static void toogle(Long id) {
    notFoundIfNull(id);
    val timeSlot = timeSlotDao.byId(id).orNull();
    notFoundIfNull(timeSlot);
    
    if (timeSlot.office != null) {
      rules.checkIfPermitted(timeSlot.office);
    } else {
      rules.checkAction("TimeSlots.tooglePredefined");
    }
    
    timeSlot.disabled = !timeSlot.disabled;
    timeSlot.save();

    flash.success("%s fascia oraria %s", timeSlot.disabled ? "Disabilita" : "Abilitata", 
        timeSlot.getLabel());  
    
    if (timeSlot.office != null) {
      manageOfficeTimeSlots(timeSlot.office.id);  
    } else {
      manageTimeSlots(null);
    }    
  }
  
  public static void changeTimeSlotToAll(Long tsId, Long officeId) {
    todo();
  }
}
