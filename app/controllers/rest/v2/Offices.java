package controllers.rest.v2;

import com.google.common.base.Optional;
import controllers.Resecure;
import dao.OfficeDao;
import helpers.JsonResponse;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import models.Office;
import play.mvc.Controller;
import play.mvc.Util;
import play.mvc.With;

@Slf4j
@With(Resecure.class)
public class Offices extends Controller {

  @Inject
  static OfficeDao officeDao;
  
  /**
   * Cerca l'ufficio in funzione dei parametri passati.
   * La ricerca viene fatta in funzione dei parametri passati
   * che possono essere null, nell'ordine id, code, codeId.
   * 
   * @param id identificativo in ePAS dell'ufficio
   * @param code codice cds dell'ufficio
   * @param codeId sedeId di attestati
   * @return l'Ufficio se trovato, altrimenti torna direttamente 
   *     una risposta HTTP 404.
   * 
   */
  @Util
  public static Office getOfficeFromRequest(
      Long id, String code, String codeId) {
    if (id == null && code == null && codeId == null) {
      JsonResponse.badRequest();
    }
    Optional<Office> office = officeDao.byIdOrCodeOrCodeId(id, code, codeId);

    if (!office.isPresent()) {
      log.info("Non trovato l'ufficio in base ai parametri passati: "
          + "id = {}, code = {}, codeId = {}", 
          id, code, codeId);
      JsonResponse.notFound("Non è stato possibile individuare l'ufficio in ePAS con "
          + "i parametri passati.");
    }

    return office.get();
  }
}
