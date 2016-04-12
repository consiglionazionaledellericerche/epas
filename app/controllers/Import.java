package controllers;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

import cnr.sync.consumers.PeopleConsumer;
import cnr.sync.dto.PersonDto;
import cnr.sync.dto.SimplePersonDto;
import cnr.sync.manager.RestOfficeManager;

import dao.OfficeDao;

import lombok.extern.slf4j.Slf4j;

import models.Office;

import play.cache.Cache;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import synch.perseoconsumers.office.OfficePerseoConsumer;
import synch.perseoconsumers.office.PerseoOffice;

@Slf4j
@With({Resecure.class, RequestInit.class})
public class Import extends Controller {

  private static final String IMPORTED_OFFICES = "importedOffices";
  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static OfficePerseoConsumer officeConsumer;
  @Inject
  private static PeopleConsumer peopleConsumer;
  @Inject
  private static RestOfficeManager restOfficeManager;

  /**
   * Visualizza la pagina principale importazione delle sedi perseo.
   */
  public static void officeList() {

    List<PerseoOffice> importedOffices = Lists.newArrayList();

    /*
    try {
      importedOffices = officeConsumer.getOffices().get();
    } catch (IllegalStateException | InterruptedException
        | ExecutionException e) {
      flash.error("Impossibile recuperare la lista degli istituti.");
      e.printStackTrace();
    }
    */

    Cache.add(IMPORTED_OFFICES, importedOffices);

    List<Office> allOffices = Office.findAll();
    List<String> officeCodes = Lists.newArrayList();

    for (Office o : allOffices) {
      if (!Strings.isNullOrEmpty(o.code)) {
        officeCodes.add(o.code);
      }
    }
    render(importedOffices, officeCodes);
  }

  /**
   * Azione di importazione sedi parametro offices.
   * @param offices
   */
  public static void importOffices(@Required final List<Integer> offices) {

    if (Validation.hasErrors()) {
      flash.error("Selezionare almeno una Sede da importare");
      officeList();
    }

    List<PerseoOffice> importedOffices = Cache.get(IMPORTED_OFFICES, List.class);

    /*
    if (importedOffices == null) {
      try {
        importedOffices = officeConsumer.getOffices().get();
      } catch (IllegalStateException | InterruptedException | ExecutionException e) {
        log.warn("Impossibile importare la lista delle sedi dall'anagrafica");
      }
    }
    */

    /*
    // Filtro la lista di tutti gli uffici presenti su perseo, lasciando solo i
    // selezionati nella form
    Collection<PerseoOffice> filteredOffices =
        Collections2.filter(importedOffices,
            new Predicate<PerseoOffice>() {
              @Override
              public boolean apply(PerseoOffice input) {
                return offices.contains(input.id);
              }
            }
        );
*/
    /*
    int synced = restOfficeManager.saveImportedSeats(filteredOffices);
    if (synced == 0) {
      flash.error("Non è stato possibile importare/sincronizzare alcuna sede, controllare i log");
    } else {
      flash.success("Importate/sincronizzate correttamente %s Sedi", synced);
    }
    */
    Institutes.index();
  }

  public static void syncSeatPeople(@Required Long id) {
    if (Validation.hasErrors()) {
      flash.error("Impossibile effettuare la sincronizzazione della sede");
      Institutes.index();
    }
    Office seat = officeDao.getOfficeById(id);

    List<SimplePersonDto> people = Lists.newArrayList();

    try {
      people = peopleConsumer.getPeople().get();
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (ExecutionException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
      
    for (SimplePersonDto person : people) {
        try {
          PersonDto personDto = peopleConsumer.getPerson(person.id).get();
          if (personDto.department.equals(seat.codeId)) {
            log.info("Beccata");
          }
        } catch (IllegalStateException | InterruptedException | ExecutionException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        
    }
      
      
   
    
    renderText("ok");

  }
}
