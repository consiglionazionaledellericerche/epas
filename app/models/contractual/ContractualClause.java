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

package models.contractual;

import com.google.common.collect.Sets;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import models.absences.CategoryGroupAbsenceType;
import models.base.PeriodModel;
import models.enumerate.ContractualClauseContext;
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
 * @author Cristian Lucchesi
 * @author Dario Tagliaferri
 */
@Getter
@Setter
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
  private String name;

  /**
   * Tempi di fruizione.
   */
  @Column(name = "fruition_time")
  private String fruitionTime;
  
  /**
   * Caratteristiche Giuridico Economiche.
   */
  @Column(name = "legal_and_economic")
  private String legalAndEconomic;  
  
  /**
   * Documentazione giustificativa. 
   */
  @Column(name = "supporting_documentation")
  private String supportingDocumentation;
  
  /**
   * Modalità di richiesta. 
   */
  @Column(name = "how_to_request")
  private String howToRequest;

  /**
   * Altre informazioni. 
   */
  @Column(name = "other_infos")
  private String otherInfos;

  @NotNull
  @Required
  @Enumerated(EnumType.STRING)
  private ContractualClauseContext context;
  
  @OneToMany(mappedBy = "contractualClause")
  private Set<CategoryGroupAbsenceType> categoryGroupAbsenceTypes = Sets.newHashSet();
  
  /**
   * Eventuali allegati o url di documentazione online.
   */
  @ManyToMany
  @JoinTable(name = "contractual_clauses_contractual_references",
      joinColumns = @JoinColumn(name = "contractual_clauses_id"), 
      inverseJoinColumns = @JoinColumn(name = "contractual_references_id"))
  private Set<ContractualReference> contractualReferences = Sets.newHashSet();

}
