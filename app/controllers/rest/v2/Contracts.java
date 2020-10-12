package controllers.rest.v2;

import cnr.sync.dto.v2.ContractCreateDto;
import cnr.sync.dto.v2.ContractShowDto;
import cnr.sync.dto.v2.ContractShowTerseDto;
import cnr.sync.dto.v2.ContractUpdateDto;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.gson.GsonBuilder;
import controllers.Resecure;
import dao.ContractDao;
import helpers.JsonResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.ContractManager;
import models.WorkingTimeType;
import org.joda.time.LocalDate;
import play.mvc.Controller;
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
  
  /**
   * Contratti di una persona.
   * La persona è individuata tramite una delle chiavi della persona passate come
   * parametro (uniformemente agli metodi REST sulle persone). 
   */
  public static void byPerson(Long id, String email, String eppn, Long personPerseoId, 
      String fiscalCode) {
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
    if (id == null) {
      JsonResponse.notFound();
    }
    val contract = contractDao.byId(id);
    if (contract == null) {
      JsonResponse.notFound();
    }
    rules.checkIfPermitted(contract.person.office);
    renderJSON(gsonBuilder.create().toJson(ContractShowTerseDto.build(contract)));
  }
  
  /**
   * Crea un contratto con i valori passati via JSON.
   * Questo metodo può essere chiamato solo in HTTP POST.
   */
  public static void create(String body) 
      throws JsonParseException, JsonMappingException, IOException {
    Verify.verify(request.method.equalsIgnoreCase("POST"));
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

    contractManager.properContractCreate(contract, workingTimeType, true);

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
    Verify.verify(request.method.equalsIgnoreCase("PUT"));
    if (id == null) {
      JsonResponse.notFound();
    }
    log.debug("Update contract -> request.body = {}", body);
    val contract = contractDao.byId(id);
    if (contract == null) {
      JsonResponse.notFound();
    }

    //Controlla anche che l'utente corrente abbia
    //i diritti di gestione anagrafica sull'office attuale 
    //della persona
    rules.checkIfPermitted(contract.person.office);

    val gson = gsonBuilder.create();
    val contractDto = gson.fromJson(body, ContractUpdateDto.class); 
    val validationResult = validation.valid(contractDto); 
    if (!validationResult.ok) {
      JsonResponse.badRequest(validation.errorsMap().toString());
    }

    contractDto.update(contract);

    //Controlla anche che l'utente corrente abbia
    //i diritti di gestione anagrafica sull'office associato alla
    //persona indicata nel DTO
    rules.checkIfPermitted(contract.person.office);

    if (!validation.valid(contract).ok) {
      JsonResponse.badRequest(validation.errorsMap().toString());
    }
    contractManager.properContractUpdate(contract, LocalDate.now(), false);

    log.info("Updated contract {} via REST", contract);
    renderJSON(gson.toJson(ContractShowDto.build(contract)));
  }

  /**
   * Effettua la cancellazione di un contratto individuata con i 
   * parametri HTTP passati.
   * Questo metodo può essere chiamato solo via HTTP DELETE.
   */
  public static void delete(Long id) {
    Verify.verify(request.method.equalsIgnoreCase("DELETE"));
    if (id == null) {
      JsonResponse.notFound();
    }
    val contract = contractDao.byId(id);
    if (contract == null) {
      JsonResponse.notFound();
    }    
    rules.checkIfPermitted(contract.person.office);

    contract.delete();
    log.info("Deleted contract {} via REST", contract);
    JsonResponse.ok();
  }
}
