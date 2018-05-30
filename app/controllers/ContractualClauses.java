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
    val categoryGroupAbsenceTypes = contractualClause.categoryGroupAbsenceTypes;
    val contractualReferences = contractualClause.contractualReferences;
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
    contractualClause.contractualReferences = contractualReferences;
    contractualClause.save();
    contractualClause.categoryGroupAbsenceTypes.stream().forEach(cgat -> {
      cgat.contractualClause = null;
      cgat.save();
    });;
    if (categoryGroupAbsenceTypes != null) {
      categoryGroupAbsenceTypes.stream().forEach(cgat -> { 
        cgat.contractualClause = contractualClause;
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
    if (!contractualClause.categoryGroupAbsenceTypes.isEmpty()) {
      flash.error("Non è possibile eliminare un istituto contrattuale associato "
          + "a categorie di tipi di assenza.");
      edit(contractualClauseId);
    }
    contractualClause.delete();
    flash.success("Operazione effettuata.");
    list();
  }
  
}
