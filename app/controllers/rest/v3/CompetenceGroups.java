package controllers.rest.v3;

import cnr.sync.dto.v3.CompetenceCodeGroupShowDto;
import cnr.sync.dto.v3.CompetenceCodeGroupShowTerseDto;
import com.google.gson.GsonBuilder;
import controllers.Resecure;
import dao.CompetenceCodeDao;
import helpers.JsonResponse;
import helpers.rest.RestUtils;
import helpers.rest.RestUtils.HttpMethod;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.val;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

@With(Resecure.class)
public class CompetenceGroups extends Controller {


  @Inject 
  static SecurityRules rules;
  @Inject
  static GsonBuilder gsonBuilder;
  @Inject
  static CompetenceCodeDao competenceCodeDao;

  /**
   * Metodo Rest che ritorna il Json con la lista dei gruppi di codici
   * di competenza presenti nel sistema, con alcune informazioni
   * di base per ogni gruppo.
   */
  public static void list() {
    RestUtils.checkMethod(request, HttpMethod.GET);
    renderJSON(gsonBuilder.create().toJson(
        competenceCodeDao.getAllGroups().stream()
          .map(ccg -> CompetenceCodeGroupShowTerseDto.build(ccg))
          .collect(Collectors.toList())
          ));
  }
  
  /**
   * Restituisce un json con le informazioni relative ad un gruppo
   * individuato tramite il suo id. 
   */
  public static void show(Long id) {
    RestUtils.checkMethod(request, HttpMethod.GET);
    if (id == null) {
      JsonResponse.badRequest("Il parametro id è obbligatorio");
    }
    val ccg = competenceCodeDao.getGroupById(id);
    RestUtils.checkIfPresent(ccg);

    renderJSON(gsonBuilder.create().toJson(
        CompetenceCodeGroupShowDto.build(ccg)));
  }
}
