/*
 * Copyright (C) 2022  Consiglio Nazionale delle Ricerche
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
package controllers.rest.v3;

import cnr.sync.dto.v3.BlockMealTicketCreateDto;
import cnr.sync.dto.v3.BlockMealTicketShowTerseDto;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gdata.util.common.base.Preconditions;
import com.google.gson.GsonBuilder;
import common.security.SecurityRules;
import controllers.Resecure;
import controllers.Security;
import controllers.rest.v2.Persons;
import dao.ContractDao;
import dao.MealTicketDao;
import dao.PersonDao;
import dao.wrapper.IWrapperFactory;
import helpers.JsonResponse;
import helpers.rest.RestUtils;
import helpers.rest.RestUtils.HttpMethod;
import it.cnr.iit.epas.DateInterval;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.inject.Inject;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import manager.ConsistencyManager;
import manager.services.mealtickets.BlockMealTicket;
import manager.services.mealtickets.IMealTicketsService;
import manager.services.mealtickets.MealTicketRecap;
import manager.services.mealtickets.MealTicketStaticUtility;
import models.Contract;
import models.MealTicket;
import models.Person;
import models.enumerate.BlockType;
import org.joda.time.LocalDate;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.Util;
import play.mvc.With;

/**
 * API Rest per la gestione delle informazioni sui buoni pasto.
 *
 * @author Cristian Lucchesi
 * @author Loredana Sideri
 */
@With(Resecure.class)
@Slf4j
public class MealTickets extends Controller {

    @Inject
    private static IWrapperFactory wrapperFactory;
    @Inject
    private static ContractDao contractDao;
    @Inject
    private static SecurityRules rules;
    @Inject
    private static IMealTicketsService mealTicketService;
    @Inject
    private static MealTicketDao mealTicketDao;
    @Inject
    private static PersonDao personDao;
    @Inject
    static ConsistencyManager consistencyManager;
    @Inject
    static GsonBuilder gsonBuilder;

    /**
     * Metodo Rest che ritorna il Json con la lista blocchetti
     * di buoni pasto consegnati ad un persona per un contratto
     * specifico.
     */
    public static void list(Long contractId) {
        RestUtils.checkMethod(request, HttpMethod.GET);
        val contract = contractDao.getContractById(contractId);
        RestUtils.checkIfPresent(contract);
        rules.checkIfPermitted(contract.person.office);

        // riepilogo contratto corrente
        Optional<MealTicketRecap> currentRecap = mealTicketService.create(contract);
        Preconditions.checkState(currentRecap.isPresent());
        MealTicketRecap recap = currentRecap.get();
        val blockMealTickets = recap.getBlockMealTicketReceivedDeliveryDesc();

        renderJSON(gsonBuilder.create().toJson(blockMealTickets.stream().map(
                bmt -> BlockMealTicketShowTerseDto.build(bmt)).collect(Collectors.toList())));
    }

    /**
     * Restituisce il JSON con il blocchetto di buoni pasto
     * dato un codice blocco consegnato ad una persona
     * per un contratto specifico.
     */
    public static void show(Long contractId, String codeBlock) {
        RestUtils.checkMethod(request, HttpMethod.GET);
        RestUtils.checkIfPresent(contractId);
        RestUtils.checkIfPresent(codeBlock);

        val contract = contractDao.getContractById(contractId);
        RestUtils.checkIfPresent(contract);
        rules.checkIfPermitted(contract.person.office);

        if (codeBlock == null || codeBlock.isEmpty()) {
            JsonResponse.notFound();
        }

        List<MealTicket> mealTicketList = mealTicketDao.getMealTicketsMatchCodeBlock(codeBlock, Optional.of(contract.person.office));

        if (mealTicketList.size() <= 0) {
            JsonResponse.notFound();
        }

        List<BlockMealTicket> blocks = MealTicketStaticUtility
                .getBlockMealTicketFromOrderedList(mealTicketList, Optional.<DateInterval>absent());

        renderJSON(gsonBuilder.create().toJson(blocks.stream().map(
                bmt -> BlockMealTicketShowTerseDto.build(bmt)).collect(Collectors.toList())));
    }

    /**
     * Metodo Rest per permettere l'inserimento di un blocchetto di buoni pasto per una persona e
     * per un contratto specifico.
     */
    public static void create(String body)
            throws JsonParseException, JsonMappingException, IOException {

        RestUtils.checkMethod(request, HttpMethod.POST);
        log.debug("Create blockMealTickets -> request.body = {}", body);

        // Malformed Json (400)
        if (body == null) {
            JsonResponse.badRequest();
        }

        val gson = gsonBuilder.create();
        val blockMealTicketCreateDto = gson.fromJson(body, BlockMealTicketCreateDto.class);

        val contractId = blockMealTicketCreateDto.getContractId();
        if (contractId == null) {
            JsonResponse.badRequest("Il contractId è obbligatorio per l'inserimento del blocchetto dei buoni pasto ");
        }

        val contract = contractDao.getContractById(contractId);
        RestUtils.checkIfPresent(contract);

        _create(blockMealTicketCreateDto, contract);
    }


    /**
     * Metodo Rest per permettere l'inserimento di un blocchetto di buoni pasto per una persona.
     * La persona è individuate tramite una delle chiavi della persona passate nel payload
     *
     */
    public static void createByPerson(String body)
            throws JsonParseException, JsonMappingException, IOException {

        RestUtils.checkMethod(request, HttpMethod.POST);
        log.debug("Create blockMealTickets byPerson -> request.body = {}", body);

        // Malformed Json (400)
        if (body == null) {
            JsonResponse.badRequest();
        }

        val gson = gsonBuilder.create();
        val blockMealTicketCreateDto = gson.fromJson(body, BlockMealTicketCreateDto.class);
        
        val person = Persons.getPersonFromRequest(
            blockMealTicketCreateDto.getPersonId(), 
            blockMealTicketCreateDto.getEmail(), 
            blockMealTicketCreateDto.getEppn(), 
            blockMealTicketCreateDto.getPersonPerseoId(),
            blockMealTicketCreateDto.getFiscalCode(), 
            blockMealTicketCreateDto.getNumber());

        Optional<Contract> contract = wrapperFactory.create(person).getCurrentContract();
        RestUtils.checkIfPresent(contract.orNull());

        _create(blockMealTicketCreateDto, contract.get());
    }

    /**
     * Creazione dei meal ticket associati alle informazioni passate.
     * 
     * @param blockMealTicketCreateDto
     * @param contract i meal ticket vengono associati a questo contratto 
     */
    static void _create(BlockMealTicketCreateDto blockMealTicketCreateDto, 
        Contract contract) {

      RestUtils.checkIfPresent(contract);
      val person = contract.person;
      rules.checkIfPermitted(person.office);
      
      val validationResult = validation.valid(blockMealTicketCreateDto);
      if (!validationResult.ok) {
          JsonResponse.badRequest(validation.errorsMap().toString());
      }

      List<MealTicket> matchingAlreadyExisting = matchingAlreadyExisting(blockMealTicketCreateDto, person);

      if (!matchingAlreadyExisting.isEmpty()) {
        JsonResponse.badRequest("Meal ticket(s) already exists: ".concat( 
            Joiner.on(", ").join(matchingAlreadyExisting.stream().map(ml -> ml.code).collect(Collectors.toList()))));
      }

      val admin = extractAdmin(blockMealTicketCreateDto);

      if (blockMealTicketCreateDto.getFirst() > blockMealTicketCreateDto.getLast()) {
          JsonResponse.badRequest("Numeri di blocchetto non validi, first > last");
      }

      // riepilogo contratto corrente
      Optional<MealTicketRecap> currentRecap = mealTicketService.create(contract);
      if (!currentRecap.isPresent()) {
          JsonResponse.notFound();
      }

      val codeBlock = blockMealTicketCreateDto.getCodeBlock();
      val blockType = blockMealTicketCreateDto.getBlockType();
      val first = blockMealTicketCreateDto.getFirst();
      val last = blockMealTicketCreateDto.getLast();
      val expireDate = blockMealTicketCreateDto.getExpiredDate();
      val deliveryDate = blockMealTicketCreateDto.getDeliveryDate();

      List<MealTicket> ticketToAddOrdered = Lists.newArrayList();
      ticketToAddOrdered.addAll(mealTicketService.buildBlockMealTicket(codeBlock, blockType,
              first, last, expireDate, person.office));

      ticketToAddOrdered.forEach(ticket -> {
          ticket.contract = contract;
          ticket.date = deliveryDate;
          ticket.admin = admin;
          validation.valid(ticket);
      });

      if (Validation.hasErrors()) {
          JsonResponse.badRequest(validation.errorsMap().toString());
      }

      Set<Contract> contractUpdated = Sets.newHashSet();

      //Persistenza
      for (MealTicket mealTicket : ticketToAddOrdered) {
          mealTicket.date = deliveryDate;
          mealTicket.contract = contract;
          mealTicket.admin = admin;
          mealTicket.save();
      }
      consistencyManager.updatePersonRecaps(person.id, deliveryDate);
      log.info("Added new mealTickets {} via REST", contractUpdated.size());

      JsonResponse.ok();
    }

    /**
     * @param blockMealTicketCreateDto i buoni pasto da verificare se già presenti
     * @param person la persona di cui controllare i buoni pasto
     * @return la lista dei Meal Ticket già presenti che corrispondono a quelli indicati nel blockMealTicketCreateDto
     */
    @Util
    static List<MealTicket> matchingAlreadyExisting(BlockMealTicketCreateDto blockMealTicketCreateDto, Person person) {
      val alreadyPresentMealTickets = 
          mealTicketDao.getMealTicketsMatchCodeBlock(
              blockMealTicketCreateDto.getCodeBlock(), Optional.of(person.office));

      List<MealTicket> matchingAlreadyExisting = Lists.newArrayList();
      IntStream.range(blockMealTicketCreateDto.getFirst(), blockMealTicketCreateDto.getLast() + 1).forEachOrdered(n -> {
        matchingAlreadyExisting.addAll(alreadyPresentMealTickets.stream()
           .filter(ml -> ml.code.equals(String.format("%s%02d", blockMealTicketCreateDto.getCodeBlock(), n)))
           .collect(Collectors.toList()));
      });
      return matchingAlreadyExisting;
    }

    
    /**
     * Decide quale utente impostare come chi ha consegnato il buono.
     * Se è passato il parametro adminId cerca l'utente con quel parametro,
     * altrimenti cerca se l'utente in sessione ha una persona associata, allora utilizza
     * quello, infine se non può determinare l'utente restituisce un 404.
     *
     * @param blockMealTicketCreateDto
     * @return la Person che consegna il buono
     */
    @Util
    static Person extractAdmin(BlockMealTicketCreateDto blockMealTicketCreateDto) {
      Long adminId = null;
      if (blockMealTicketCreateDto.getAdminId() != null) {
        val person = personDao.byId(blockMealTicketCreateDto.getAdminId());
        if (!person.isPresent()) {
          JsonResponse.notFound(String.format("Admin con id %s non trovato", 
              blockMealTicketCreateDto.getAdminId()));
        } else {
          adminId = person.get().id;
        }
      }
      if (adminId != null && Security.getUser().get().person != null) {
        adminId = Security.getUser().get().person.id;
      }
      if (adminId == null) {
          JsonResponse.notFound("Admin non trovato per effettuare l'inserimento");
      }

      return personDao.byId(adminId).get();
    }

    /**
     * Metodo Rest per effettuare l'eliminazione di un blocchetto di buoni pasto
     * consegnati ad un persona per un contratto specifico.
     * Questo metodo può essere chiamato solo via HTTP DELETE.
     */
    public static void delete(Long contractId, String codeBlock, int first, int last) {
        RestUtils.checkMethod(request, HttpMethod.DELETE);
        val contract = contractDao.getContractById(contractId);
        RestUtils.checkIfPresent(contract);
        rules.checkIfPermitted(contract.person.office);

        List<MealTicket> mealTicketList = mealTicketDao.getMealTicketsInCodeBlock(codeBlock, Optional.fromNullable(contract));

        Preconditions.checkState(mealTicketList.size() > 0);

        List<MealTicket> mealTicketToRemove = MealTicketStaticUtility
                .blockPortion(mealTicketList, contract, first, last);

        int deleted = 0;
        LocalDate pastDate = LocalDate.now();

        for (MealTicket mealTicket : mealTicketToRemove) {
            if (mealTicket.date.isBefore(pastDate)) {
                pastDate = mealTicket.date;
            }

            mealTicket.delete();
            log.info("Deleted mealTicket {} via REST", mealTicket);
            deleted++;
        }

        consistencyManager.updatePersonSituation(contract.person.id, pastDate);
        log.info("Deleted {} mealTickets via REST", deleted);

        JsonResponse.ok();
    }

    /**
     * Metodo Rest per effettuare la conversione della tipologia di blocchetto di buoni pasto da cartaceo a elettronico
     * o viceversa.
     * Questo metodo può essere chiamato solo via HTTP PUT.
     */
    public static void convert(Long contractId, String codeBlock) {
        RestUtils.checkMethod(request, HttpMethod.PUT);
        val contract = contractDao.getContractById(contractId);
        RestUtils.checkIfPresent(contract);
        rules.checkIfPermitted(contract.person.office);

        if (codeBlock == null || codeBlock.isEmpty()) {
            JsonResponse.notFound();
        }

        List<MealTicket> mealTicketList = mealTicketDao.getMealTicketsMatchCodeBlock(codeBlock, Optional.of(contract.person.office));
        if (mealTicketList.size() <= 0) {
            JsonResponse.notFound();
        }

        int converted = 0;
        for (MealTicket mealTicket : mealTicketList) {
            if (mealTicket.blockType.equals(BlockType.papery)) {
                mealTicket.blockType = BlockType.electronic;
            } else {
                mealTicket.blockType = BlockType.papery;
            }
            mealTicket.save();
            converted++;
        }

        log.info("Converted {} mealTickets via REST", converted);

        JsonResponse.ok();
    }


    /**
     * Metodo Rest per effettuare la riconsegna del blocchetto di buoni pasto alla sede centrale.
     * Questo metodo può essere chiamato solo via HTTP PUT.
     */
    public static void returnBlock(Long contractId, String codeBlock, int first, int last) {
        RestUtils.checkMethod(request, HttpMethod.PUT);
        val contract = contractDao.getContractById(contractId);
        RestUtils.checkIfPresent(contract);
        rules.checkIfPermitted(contract.person.office);

        if (codeBlock == null || codeBlock.isEmpty()) {
            JsonResponse.notFound();
        }

        List<MealTicket> mealTicketList = mealTicketDao.getMealTicketsMatchCodeBlock(codeBlock, Optional.of(contract.person.office));
        if (mealTicketList.size() <= 0) {
            JsonResponse.notFound();
        }

        int returned = 0;
        List<MealTicket> blockPortionToReturn = MealTicketStaticUtility
                .blockPortion(mealTicketList, contract, first, last);

        for (MealTicket mealTicket : blockPortionToReturn) {
            mealTicket.returned = true;
            returned++;
        }

        // Perform
        LocalDate pastDate = LocalDate.now();
        for (MealTicket mealTicket : mealTicketList) {
            if (mealTicket.date.isBefore(pastDate)) {
                pastDate = mealTicket.date;
            }
        }
        for (MealTicket mealTicket : blockPortionToReturn) {
            if (mealTicket.date.isBefore(pastDate)) {
                pastDate = mealTicket.date;
            }
            mealTicket.save();
        }
        consistencyManager.updatePersonSituation(contract.person.id, pastDate);

        log.info("Returned {} mealTickets via REST", returned);

        JsonResponse.ok();
    }

}
