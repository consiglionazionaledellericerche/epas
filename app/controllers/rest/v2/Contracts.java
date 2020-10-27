package controllers.rest.v2;

import cnr.sync.dto.v2.ContractCreateDto;
import cnr.sync.dto.v2.ContractShowDto;
import cnr.sync.dto.v2.ContractShowTerseDto;
import cnr.sync.dto.v2.ContractUpdateDto;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.base.Optional;
import com.google.gson.GsonBuilder;
import controllers.Resecure;
import controllers.rest.v2.RestUtil.HttpMethod;
import dao.ContractDao;
import dao.PersonDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import helpers.JsonResponse;
import it.cnr.iit.epas.DateInterval;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.ContractManager;
import manager.PeriodManager;
import manager.recaps.recomputation.RecomputeRecap;
import models.Contract;
import models.WorkingTimeType;
import org.joda.time.LocalDate;
import play.mvc.Controller;
import play.mvc.Util;
import play.mvc.With;
import security.SecurityRules;

@Slf4j
@With(Resecure.class)
public class Contracts extends Controller {
 
  @Inject
  static ContractDao contractDao;
  @Inject
  static ContractManager contractManager;
  @Inject 
  static SecurityRules rules;
  @Inject
  static GsonBuilder gsonBuilder;
  @Inject
  static IWrapperFactory wrapperFactory;
  @Inject
  static PeriodManager periodManager;
  @Inject
  static PersonDao personDao;
  
  /**
   * Contratti di una persona.
   * La persona è individuata tramite una delle chiavi della persona passate come
   * parametro (uniformemente ai metodi REST sulle persone). 
   */
  public static void byPerson(Long id, String email, String eppn, Long personPerseoId, 
      String fiscalCode) {
    RestUtil.checkMethod(request, HttpMethod.GET);
    val person = Persons.getPersonFromRequest(id, email, eppn, personPerseoId, fiscalCode);
    rules.checkIfPermitted(person.office);
    List<ContractShowTerseDto> contracts = 
        person.contracts.stream().map(c -> ContractShowTerseDto.build(c))
        .collect(Collectors.toList());
    renderJSON(gsonBuilder.create().toJson(contracts));
  }
  
  /**
   * Restituisce il JSON con il contratto cercato per id. 
   */
  public static void show(Long id) {
    RestUtil.checkMethod(request, HttpMethod.GET);
    val contract = getContractFromRequest(id);
    renderJSON(gsonBuilder.create().toJson(ContractShowDto.build(contract)));
  }
  
  /**
   * Crea un contratto con i valori passati via JSON.
   * Questo metodo può essere chiamato solo in HTTP POST.
   */
  public static void create(String body) 
      throws JsonParseException, JsonMappingException, IOException {
    RestUtil.checkMethod(request, HttpMethod.POST);
    log.debug("Create contract -> request.body = {}", body);
    if (body == null) {
      JsonResponse.badRequest();
    }
    val gson = gsonBuilder.create();
    val contractDto = gson.fromJson(body, ContractCreateDto.class); 
    val validationResult = validation.valid(contractDto); 
    if (!validationResult.ok) {
      JsonResponse.badRequest(validation.errorsMap().toString());
    }

    val contract = ContractCreateDto.build(contractDto);
    if (!validation.valid(contract).ok) {
      JsonResponse.badRequest(validation.errorsMap().toString());
    }

    //Controlla anche che l'utente corrente abbia
    //i diritti di gestione anagrafica sull'office associato alla
    //persona indicata nel DTO
    rules.checkIfPermitted(contract.person.office);

    Optional<WorkingTimeType> workingTimeType =  
        contractDto.getWorkingTimeTypeId() == null 
          ? Optional.absent()
            : Optional.fromNullable(WorkingTimeType.findById(contractDto.getWorkingTimeTypeId()));

    contract.person = personDao.getPersonById(contractDto.getPersonId());
    if (!contractManager.properContractCreate(contract, workingTimeType, true)) {
      JsonResponse.badRequest("Problemi nella creazione del contratto, verificare le date");
    }

    log.info("Created contract {} via REST", contract);
    renderJSON(gson.toJson(ContractShowDto.build(contract)));
  }

  /**
   * Aggiorna i dati di un contratto individuato per id
   * con i valori passati nel body HTTP come JSON.
   * Questo metodo può essere chiamato solo via HTTP PUT.
   */
  public static void update(Long id, String body) 
      throws JsonParseException, JsonMappingException, IOException {
    RestUtil.checkMethod(request, HttpMethod.PUT);
    val contract = getContractFromRequest(id);

    val gson = gsonBuilder.create();
    val contractDto = gson.fromJson(body, ContractUpdateDto.class); 
    val validationResult = validation.valid(contractDto); 
    if (!validationResult.ok) {
      JsonResponse.badRequest(validation.errorsMap().toString());
    }

    // Salvo la situazione precedente per capire da dove riaggiornare i riepiloghi
    IWrapperContract wrappedContract = wrapperFactory.create(contract);
    final DateInterval previousInterval = wrappedContract.getContractDatabaseInterval();
    
    contractDto.update(contract);

    //Controlla anche che l'utente corrente abbia
    //i diritti di gestione anagrafica sull'office associato alla
    //persona indicata nel DTO
    rules.checkIfPermitted(contract.person.office);

    if (!validation.valid(contract).ok) {
      JsonResponse.badRequest(validation.errorsMap().toString());
    }

    DateInterval newInterval = wrappedContract.getContractDatabaseInterval();
    RecomputeRecap recomputeRecap = periodManager.buildTargetRecap(previousInterval, newInterval,
        wrappedContract.initializationMissing());

    contractManager.properContractUpdate(
        contract, Optional.fromNullable(recomputeRecap.recomputeFrom).or(LocalDate.now()), false);

    log.info("Updated contract {} via REST", contract);
    renderJSON(gson.toJson(ContractShowDto.build(contract)));
  }

  /**
   * Imposta il collegamento al contratto precedente del contratto individuato con li parametro id.
   * @param id id del contratto di cui cambiare i riferimenti al contratto precedente
   */
  public void setPreviousContract(Long id) {
    applyPreviousContract(id, true);
  }

  /**
   * Rimuove il collegamento al contratto precedente del contratto individuato con li parametro id.
   * @param id id del contratto di cui cambiare i riferimenti al contratto precedente
   */
  public void unsetPreviousContract(Long id) {
    applyPreviousContract(id, false);
  }

  /**
   * Imposta o rimuove il collegamento al contratto precedente del 
   * contratto individuato con li parametro id.
   * @param id id del contratto di cui cambiare i riferimenti al contratto precedente
   * @param linkedToPreviousContract passare true per impostare il contratto precedente, passare 
   *     false altrimenti.
   */
  @Util
  private void applyPreviousContract(Long id, boolean linkedToPreviousContract) {
    RestUtil.checkMethod(request, HttpMethod.PUT);
    val contract = getContractFromRequest(id);  

    if (!contractManager.applyPreviousContractLink(contract, linkedToPreviousContract)) {
      JsonResponse.badRequest("Non esiste alcun contratto precedente cui linkare il contratto "
          + "attuale");
    }
    
    log.info("Updated contract {} set previous contract to {} via REST", 
        contract, linkedToPreviousContract);
    renderJSON(gsonBuilder.create().toJson(ContractShowDto.build(contract)));
  }
  
  /**
   * Effettua la cancellazione di un contratto individuata con i 
   * parametri HTTP passati.
   * Questo metodo può essere chiamato solo via HTTP DELETE.
   */
  public static void delete(Long id) {
    RestUtil.checkMethod(request, HttpMethod.DELETE);
    val contract = getContractFromRequest(id);

    contract.delete();
    log.info("Deleted contract {} via REST", contract);
    JsonResponse.ok();
  }
  
  /**
   * Cerca il contratto in funzione del id passato.
   * 
   * @return il contratto se trovato, altrimenti torna direttamente 
   *     una risposta HTTP 404.
   * 
   */
  @Util
  private static Contract getContractFromRequest(Long id) {
    if (id == null) {
      JsonResponse.notFound();
    }
    
    val contract = contractDao.byId(id);
    
    if (contract == null) {
      JsonResponse.notFound();
    }
    
    //Controlla anche che l'utente corrente abbia
    //i diritti di gestione anagrafica sull'office attuale 
    //della persona
    rules.checkIfPermitted(contract.person.office);
    return contract;
  }
}
