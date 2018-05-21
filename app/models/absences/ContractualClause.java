package models.absences;

import com.google.common.collect.Sets;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
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

  /**
   * Lista dei gruppi di codici di assenza legati a questi istituto contrattuale.
   * Collegare solamente i GroupAbsenceType che NON hanno un "previousGroupChecked".
   * In pratica si collegano solo gli inizi delle eventuali catene di gruppi di codici 
   * perché il resto della catena è collegato implicitamente.
   * <p>
   * Es. si collega Gruppo codice 23 che ha come gruppo successivo il Gruppo dei codici 25
   * e a sua volta il gruppo dei codici 24.
   * </p>  
   */
  @ManyToMany
  @JoinTable(name = "contractual_clauses_group_absence_types",
      joinColumns = @JoinColumn(name = "contractual_clauses_id"), 
      inverseJoinColumns = @JoinColumn(name = "group_absence_types_id"))
  public Set<GroupAbsenceType> groupAbsenceTypes = Sets.newHashSet();

  /**
   * Eventuali allegati o url di documentazione online.
   */
  @ManyToMany
  @JoinTable(name = "contractual_clauses_contractual_references",
      joinColumns = @JoinColumn(name = "contractual_clauses_id"), 
      inverseJoinColumns = @JoinColumn(name = "contractual_references_id"))
  public Set<ContractualReference> contractualReferences = Sets.newHashSet();

}
