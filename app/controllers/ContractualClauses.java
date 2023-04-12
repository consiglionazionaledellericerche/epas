/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
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

package controllers;

import com.google.common.base.Optional;
import dao.ContractualClauseDao;
import helpers.jpa.JpaReferenceBinder;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import lombok.val;
import models.absences.CategoryGroupAbsenceType;
import models.contractual.ContractualClause;
import models.contractual.ContractualReference;
import play.data.binding.As;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Controller per la gestione delle clausole contrattuli.
 *
 * @author Cristian Lucchesi
 */
@With({Resecure.class})
public class ContractualClauses extends Controller {

  @Inject
  static ContractualClauseDao contractualClauseDao;
  
  /**
   * Mostra tutti gli istituti contrattuali.
   */
  public static void list() {
    List<ContractualClause> contractualClauses = 
        contractualClauseDao.all(Optional.of(true));
    render(contractualClauses);
  }

  /**
   * Visualizzazione dell'istituto contrattuale.
   *
   * @param id id
   */
  public static void show(Long id) {
    ContractualClause contractualClause = ContractualClause.findById(id);
    notFoundIfNull(contractualClause);
    render(contractualClause);
  }

  /**
   * Nuovo istituto contrattuale.
   */
  public static void blank() {
    ContractualClause contractualClause = new ContractualClause();
    render("@edit", contractualClause);    
  }

  /**
   * Modifica dell'istituto contrattuale.
   *
   * @param contractualClauseId id
   */
  public static void edit(Long contractualClauseId) {
    ContractualClause contractualClause = ContractualClause.findById(contractualClauseId);
    notFoundIfNull(contractualClause);
    val categoryGroupAbsenceTypes = contractualClause.getCategoryGroupAbsenceTypes();
    val contractualReferences = contractualClause.getContractualReferences();
    render(contractualClause, categoryGroupAbsenceTypes, contractualReferences);
  }
  
  /**
   * Salva l'istituto contrattuale.
   *
   * @param contractualClause istituto contrattuale
   */
  public static void save(@Valid ContractualClause contractualClause, 
      @As(binder = JpaReferenceBinder.class)
      Collection<CategoryGroupAbsenceType> categoryGroupAbsenceTypes,
      @As(binder = JpaReferenceBinder.class)
      Set<ContractualReference> contractualReferences) {

    if (Validation.hasErrors()) {
      flash.error("Correggere gli errori indicati");
      render("@edit", contractualClause, categoryGroupAbsenceTypes, 
          contractualReferences);
    }
    contractualClause.setContractualReferences(contractualReferences);
    contractualClause.save();
    contractualClause.getCategoryGroupAbsenceTypes().stream().forEach(cgat -> {
      cgat.setContractualClause(null);
      cgat.save();
    });
    if (categoryGroupAbsenceTypes != null) {
      categoryGroupAbsenceTypes.stream().forEach(cgat -> { 
        cgat.setContractualClause(contractualClause);
        cgat.save();
      }); 
    }
    flash.success("Operazione eseguita.");
    show(contractualClause.id);
  }

  /**
   * Rimuove l'istituto contrattuale.
   *
   * @param contractualClauseId tab
   */
  public static void delete(Long contractualClauseId) {
    ContractualClause contractualClause = ContractualClause.findById(contractualClauseId);
    notFoundIfNull(contractualClause);
    if (!contractualClause.getCategoryGroupAbsenceTypes().isEmpty()) {
      flash.error("Non è possibile eliminare un istituto contrattuale associato "
          + "a categorie di tipi di assenza.");
      edit(contractualClauseId);
    }
    contractualClause.delete();
    flash.success("Operazione effettuata.");
    list();
  }
  
}