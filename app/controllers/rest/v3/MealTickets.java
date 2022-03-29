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
import com.google.common.base.Optional;
import com.google.gdata.util.common.base.Preconditions;
import com.google.gson.GsonBuilder;
import common.security.SecurityRules;
import controllers.Resecure;
import dao.ContractDao;
import helpers.rest.RestUtils;
import helpers.rest.RestUtils.HttpMethod;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.val;
import manager.services.mealtickets.IMealTicketsService;
import manager.services.mealtickets.MealTicketRecap;
import play.mvc.Controller;
import play.mvc.With;

/**
 * API Rest per la gestione delle informazioni sui buoni pasto.
 *
 * @author Cristian Lucchesi
 * @author Loredana Sideri
 *
 */
@With(Resecure.class)
public class MealTickets extends Controller {

    @Inject
    private static ContractDao contractDao;
    @Inject
    private static SecurityRules rules;
    @Inject
    private static IMealTicketsService mealTicketService;
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
}
