package controllers;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import cnr.sync.consumers.OfficeConsumer;
import cnr.sync.consumers.PeopleConsumer;
import cnr.sync.dto.OfficeDTO;
import cnr.sync.manager.RestOfficeManager;

import dao.OfficeDao;

import lombok.extern.slf4j.Slf4j;

import models.Office;
import models.Person;

import play.cache.Cache;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

@Slf4j
@With({Resecure.class, RequestInit.class})
public class Import extends Controller {

  private final static String IMPORTED_OFFICES = "importedOffices";
  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static OfficeConsumer officeConsumer;
  @Inject
  private static PeopleConsumer peopleConsumer;
  @Inject
  private static RestOfficeManager restOfficeManager;

  public static void officeList() {

    List<OfficeDTO> importedOffices = Lists.newArrayList();

    try {
      importedOffices = officeConsumer.getOffices().get();
    } catch (IllegalStateException | InterruptedException
            | ExecutionException e) {
      flash.error("Impossibile recuperare la lista degli istituti.");
      e.printStackTrace();
    }

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

  public static void importOffices(@Required final List<Integer> offices) {

    if (Validation.hasErrors()) {
      flash.error("Selezionare almeno una Sede da importare");
      officeList();
    }

    List<OfficeDTO> importedOffices = Cache.get(IMPORTED_OFFICES, List.class);

    if (importedOffices == null) {
      try {
        importedOffices = officeConsumer.getOffices().get();
      } catch (IllegalStateException | InterruptedException | ExecutionException e) {
        log.warn("Impossibile importare la lista delle sedi dall'anagrafica - {}", e.getStackTrace());
      }
    }

    //  Filtro la lista di tutti gli uffici presenti su perseo, lasciando solo i selezionati nella form
    Collection<OfficeDTO> filteredOffices = Collections2.filter(importedOffices,
            new Predicate<OfficeDTO>() {
              @Override
              public boolean apply(OfficeDTO input) {
                return offices.contains(input.id);
              }
            });

    int synced = restOfficeManager.saveImportedSeats(filteredOffices);
    if (synced == 0) {
      flash.error("Non è stato possibile importare/sincronizzare alcuna sede, controllare i log");
    } else {
      flash.success("Importate/sincronizzate correttamente %s Sedi", synced);
    }
    Institutes.index();
  }

  public static void syncSeatPeople(@Required Long id) {
    if (Validation.hasErrors()) {
      flash.error("Impossibile effettuare la sincronizzazione della sede");
      Institutes.index();
    }
    Office seat = officeDao.getOfficeById(id);

    Set<Person> importedPeople = Sets.newHashSet();

//		try {
//			importedPeople = peopleConsumer.seatPeople(seat.code.toString()).get();
//		} catch (IllegalStateException | InterruptedException
//				| ExecutionException e) {
//			flash.error("Impossibile recuperare la lista degli istituti da Perseo");
//			e.printStackTrace();
//		}
//		
//		for(Person p : importedPeople ){
//			Logger.info("Persone Importata: %s-%s-%s-%s-%s-%s",p.fullName(),p.birthday,p.email,p.cnr_email,p.number,p.badgeNumber);
//		}

  }
}
