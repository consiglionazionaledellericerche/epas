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

import cnr.sync.dto.v3.BlockMealTicketShowTerseDto;
import cnr.sync.dto.v3.MealTicketShowTerseDto;
import com.google.common.base.Optional;
import com.google.gdata.util.common.base.Preconditions;
import com.google.gson.GsonBuilder;
import common.security.SecurityRules;
import controllers.Resecure;
import dao.ContractDao;
import dao.MealTicketDao;
import helpers.JsonResponse;
import helpers.rest.RestUtils;
import helpers.rest.RestUtils.HttpMethod;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;

import it.cnr.iit.epas.DateInterval;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import manager.ConsistencyManager;
import manager.services.mealtickets.BlockMealTicket;
import manager.services.mealtickets.IMealTicketsService;
import manager.services.mealtickets.MealTicketRecap;
import manager.services.mealtickets.MealTicketStaticUtility;
import models.MealTicket;
import models.enumerate.BlockType;
import org.joda.time.LocalDate;
import play.mvc.Controller;
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
    private static ContractDao contractDao;
    @Inject
    private static SecurityRules rules;
    @Inject
    private static IMealTicketsService mealTicketService;
    @Inject
    private static MealTicketDao mealTicketDao;
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

        if (codeBlock == null || !codeBlock.isEmpty()) {
            JsonResponse.notFound();
        }

        List<MealTicket> mealTicketList = mealTicketDao.getMealTicketsMatchCodeBlock(codeBlock, Optional.of(contract.person.office));
        Preconditions.checkState(mealTicketList.size() > 0);

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

        if (codeBlock == null || !codeBlock.isEmpty()) {
            JsonResponse.notFound();
        }

        List<MealTicket> mealTicketList = mealTicketDao.getMealTicketsMatchCodeBlock(codeBlock, Optional.of(contract.person.office));
        Preconditions.checkState(mealTicketList.size() > 0);

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
