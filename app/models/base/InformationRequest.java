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

import com.beust.jcommander.internal.Lists;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.validation.constraints.NotNull;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDateTime;
import models.Person;
import models.enumerate.InformationType;
import models.flows.AbsenceRequestEvent;
import models.informationrequests.InformationRequestEvent;
import play.data.validation.Required;

@MappedSuperclass
public class InformationRequest extends BaseModel{
  
  @Required
  @NotNull
  @ManyToOne(optional = false)
  public Person person;
  
  @Enumerated(EnumType.STRING)
  public InformationType informationType;
  
  /**
   * Data di approvazione del responsabili sede.
   */
  @Column(name = "office_head_approved")
  public LocalDateTime officeHeadApproved;
  
  /**
   * Indica se è richieta l'approvazione da parte del responsabile di sede.
   */
  @Column(name = "office_head_approval_required")
  public boolean officeHeadApprovalRequired = true;
  
  @NotAudited
  @OneToMany(mappedBy = "absenceRequest")
  @OrderBy("createdAt DESC")
  public List<InformationRequestEvent> events = Lists.newArrayList();

}
