package cnr.sync.manager;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import cnr.sync.dto.DepartmentDTO;
import cnr.sync.dto.PersonRest;
import dao.OfficeDao;
import dao.PersonDao;
import models.Office;
import models.Person;
import play.Play;
import play.libs.WS;
import play.libs.WS.HttpResponse;

public class SyncManager {

  private final static Logger log = LoggerFactory.getLogger(SyncManager.class);
  private final OfficeDao officeDao;
  private final PersonDao personDao;
  @Inject
  public SyncManager(OfficeDao officeDao, PersonDao personDao) {
    this.officeDao = officeDao;
    this.personDao = personDao;
  }

  /**
   * questo metodo può essere chiamato dal job settimanale che sincronizza le email cnr del
   * personale oppure dalla chiamata rest per conoscere i tempi di lavoro e le missioni del
   * personale per la rendicontazione dei progetti
   */
  @Deprecated //Deprecato oppure inserire nuova chiave di sincronizzazione
  public void syncronizeCnrEmail() {
    List<Office> helpList = officeDao.getAllOffices();
    List<Office> officeList = Lists.newArrayList();
    for (Office office : helpList) {
      if (office.codeId != null)
        officeList.add(office);
    }
    int contatore = 0;
    String url = Play.configuration.getProperty("people.rest");

    String perseoUrl = Play.configuration.getProperty("perseo.department");
    for (Office office : officeList) {
      perseoUrl = perseoUrl + office.codeId;
      HttpResponse perseoResponse = WS.url(perseoUrl).get();
      Gson gson = new Gson();

      DepartmentDTO dep = gson.fromJson(perseoResponse.getJson(), DepartmentDTO.class);
      HttpResponse response = WS.url(url + dep.code).get();

      List<PersonRest> people = gson.fromJson(response.getJson().toString(),
              new TypeToken<ArrayList<PersonRest>>() {
              }.getType());
      for (PersonRest pr : people) {
        if (pr.number == null) {
          log.info("Non esiste matricola per {} {}", pr.firstname, pr.surname);
        } else {
          Person person = personDao.getPersonByNumber(pr.number);
          if (person != null) {
            // FIXME: usare un altra chiave di sincronizzazione (cnr_email non esiste più)
            //person.cnr_email = pr.email;
            person.iId = new Integer(pr.id);
            person.save();
            log.info("Salvata la mail cnr per {} {}", person.name, person.surname);
            contatore++;
          } else {
            log.info("La persona {} {} non è presente in anagrafica", pr.firstname, pr.surname);
          }
        }

      }
      perseoUrl = Play.configuration.getProperty("perseo.department");

    }
    log.info("Terminata sincronizzazione delle email cnr per {} dipendenti", contatore);
  }

}
