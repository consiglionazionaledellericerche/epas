package models.absences;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import models.base.BaseModel;
import org.apache.commons.compress.utils.Lists;
import org.hibernate.envers.Audited;

/**
 * Istituto contrattuale.
 * Contiene la documentazione delle varie disposizioni contrattuali
 * raggruppate per tipologia di assenze (GroupAbsenceType).
 * 
 * @author cristian
 * @author dario
 */
@Audited
@Entity
public class ContractualClause extends BaseModel {

  private static final long serialVersionUID = -1933483982513717538L;

  /**
   * Esempio: Permessi retribuiti (art. 72)
   */
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
   * Es. si collega Gruppo codice 25 che hanno come gruppo successivo il Gruppo dei codici 24.
   * </p>  
   */
  public List<GroupAbsenceType> groupAbsenceTypes = Lists.newArrayList();
  
  /**
   * Eventuali allegati o url di documentazione online.
   */
  @ManyToMany
  public List<ContractualReference> contractualReferences = Lists.newArrayList();
  
}
