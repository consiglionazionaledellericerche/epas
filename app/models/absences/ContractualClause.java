package models.absences;

import com.google.common.collect.Sets;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import models.base.PeriodModel;
import org.hibernate.envers.Audited;
import play.data.validation.Required;
import play.data.validation.Unique;

/**
 * Istituto contrattuale.
 * 
 * <p>
 *Contiene la documentazione delle varie disposizioni contrattuali
 * raggruppate per tipologia di assenza (GroupAbsenceType).
 * </p>
 *  
 * @author cristian
 * @author dario
 */
@Audited
@Entity
@Table(name = "contractual_clauses")
public class ContractualClause extends PeriodModel {

  private static final long serialVersionUID = -1933483982513717538L;

  /**
   * Esempio: Permessi retribuiti (art. 72)
   */
  @NotNull
  @Required
  @Unique
  public String name;

  /**
   * Descrizione completa dell'articolo contrattuale o dei rifermenti normativi. 
   */
  public String description;

  @OneToMany(mappedBy = "contractualClause")
  public Set<CategoryGroupAbsenceType> categoryGroupAbsenceTypes = Sets.newHashSet();
  
  /**
   * Eventuali allegati o url di documentazione online.
   */
  @ManyToMany
  @JoinTable(name = "contractual_clauses_contractual_references",
      joinColumns = @JoinColumn(name = "contractual_clauses_id"), 
      inverseJoinColumns = @JoinColumn(name = "contractual_references_id"))
  public Set<ContractualReference> contractualReferences = Sets.newHashSet();

}
