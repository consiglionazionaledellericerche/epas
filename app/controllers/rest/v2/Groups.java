package controllers.rest.v2;

import cnr.sync.dto.v2.AffiliationShowDto;
import cnr.sync.dto.v2.GroupCreateDto;
import cnr.sync.dto.v2.GroupShowDto;
import cnr.sync.dto.v2.GroupShowTerseDto;
import cnr.sync.dto.v2.GroupUpdateDto;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.base.Verify;
import com.google.gson.GsonBuilder;
import controllers.Resecure;
import dao.AffiliationDao;
import dao.GroupDao;
import helpers.JsonResponse;
import java.io.IOException;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

/**
 * API Rest per la gestione dei gruppi di persone.
 * 
 * @author cristian
 *
 */
@Slf4j
@With(Resecure.class)
public class Groups extends Controller {

  @Inject
  static GroupDao groupDao;
  @Inject
  static AffiliationDao affiliationDao;
  @Inject 
  static SecurityRules rules;
  @Inject
  static GsonBuilder gsonBuilder;

  /**
   * Elendo dei gruppi di una sede in formato JSON. 
   * 
   * @param id id in ePAS dell'Ufficio.
   * @param code codice cds dell'ufficio
   * @param codeId sedeId di attestati
   */
  public static void list(Long id, String code, String codeId) {
    val office = Offices.getOfficeFromRequest(id, code, codeId);
    rules.checkIfPermitted(office);

    val list = 
        office.groups.stream().map(group -> GroupShowTerseDto.build(group))
        .collect(Collectors.toSet());
    renderJSON(gsonBuilder.create().toJson(list));
  }

  /**
   * Restituisce il JSON con l'affiliazione cercata per id. 
   */
  public static void show(Long id) {
    notFoundIfNull(id);
    val affiliation = affiliationDao.byId(id).orElse(null);
    notFoundIfNull(affiliation);
    rules.checkIfPermitted(affiliation.getGroup().getOffice());
    renderJSON(gsonBuilder.create().toJson(AffiliationShowDto.build(affiliation)));
  }

  /**
   * Crea un gruppo con i valori passati via JSON.
   * Questo metodo può essere chiamato solo in HTTP POST.
   */
  public static void create(String body) 
      throws JsonParseException, JsonMappingException, IOException {
    Verify.verify(request.method.equalsIgnoreCase("POST"));

    log.debug("Create affiliation -> request.body = {}", body);

    val gson = gsonBuilder.create();
    val groupDto = gson.fromJson(body, GroupCreateDto.class); 
    val validationResult = validation.valid(groupDto); 
    if (!validationResult.ok) {
      JsonResponse.badRequest(validation.errorsMap().toString());
    }

    val group = GroupCreateDto.build(groupDto);
    if (!validation.valid(group).ok) {
      JsonResponse.badRequest(validation.errorsMap().toString());
    }

    //Controlla anche che l'utente corrente abbia
    //i diritti di gestione anagrafica sull'office indicato
    //nel DTO
    rules.checkIfPermitted(group.office);

    group.save();

    log.info("Created group {} via REST", group);
    renderJSON(gson.toJson(GroupShowDto.build(group)));
  }

  /**
   * Aggiorna i dati di un gruppo individuata con i parametri HTTP
   * passati ed i valori passati nel body HTTP come JSON.
   * Questo metodo può essere chiamato solo via HTTP PUT.
   */
  public static void update(Long id, String body) 
      throws JsonParseException, JsonMappingException, IOException {
    Verify.verify(request.method.equalsIgnoreCase("PUT"));
    
    notFoundIfNull(id);
    log.debug("Update group -> request.body = {}", body);
    val group = groupDao.byId(id).orNull();
    notFoundIfNull(group);

    //Controlla anche che l'utente corrente abbia
    //i diritti di gestione anagrafica sull'office attuale 
    //della persona
    rules.checkIfPermitted(group.office);

    val gson = gsonBuilder.create();
    val groupDto = gson.fromJson(body, GroupUpdateDto.class); 
    val validationResult = validation.valid(groupDto); 
    if (!validationResult.ok) {
      JsonResponse.badRequest(validation.errorsMap().toString());
    }

    groupDto.update(group);

    //Controlla anche che l'utente corrente abbia
    //i diritti di gestione anagrafica sull'office indicato 
    //nel DTO
    rules.checkIfPermitted(group.office);

    if (!validation.valid(group).ok) {
      JsonResponse.badRequest(validation.errorsMap().toString());
    }
    group.save();

    log.info("Updated group {} via REST", group);
    renderJSON(gson.toJson(GroupShowDto.build(group)));
  }

  /**
   * Effettua la cancellazione di una persona individuata con i 
   * parametri HTTP passati.
   * Questo metodo può essere chiamato solo via HTTP DELETE.
   */
  public static void delete(Long id) {
    Verify.verify(request.method.equalsIgnoreCase("DELETE"));
    val group = groupDao.byId(id).orNull();
    notFoundIfNull(group);
    rules.checkIfPermitted(group.office);
    
    if (!group.affiliations.isEmpty()) {
      JsonResponse.conflict(
          String.format("Ci sono %d affiliazioni di persone associate a questo gruppo. "
              + "Cancellare prima le affiliazioni delle persone.", group.affiliations.size()));
    }

    group.delete();
    log.info("Deleted group {} via REST", group);
    JsonResponse.ok();
  }
}