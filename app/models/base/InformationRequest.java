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

package models.base;

import com.google.common.collect.Lists;
import java.time.LocalDateTime;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import models.Person;
import models.enumerate.InformationType;
import models.flows.AbsenceRequestEvent;
import models.informationrequests.InformationRequestEvent;
import play.data.validation.Required;

@Audited
//@DiscriminatorColumn(name="information_type", discriminatorType = DiscriminatorType.STRING)
@Inheritance(strategy = InheritanceType.JOINED)
@Entity
@Table(name="information_requests")
public abstract class InformationRequest extends BaseModel{
  
  @Required
  @NotNull
  @ManyToOne(optional = false)
  public Person person;
  
  @Required
  @NotNull
  @Enumerated(EnumType.STRING)
  public InformationType informationType;
  
  /**
   * Data di approvazione del responsabili sede.
   */
  public LocalDateTime officeHeadApproved;
  
  /**
   * Indica se è richieta l'approvazione da parte del responsabile di sede.
   */
  public boolean officeHeadApprovalRequired = true;
  
  @NotAudited
  @OneToMany(mappedBy = "informationRequest")
  @OrderBy("createdAt DESC")
  public List<InformationRequestEvent> events = Lists.newArrayList();

}
