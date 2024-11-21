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

package models.flows;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.ToString;
import models.User;
import models.base.BaseModel;
import models.flows.enumerate.CompetenceRequestEventType;

import org.hibernate.envers.Audited;
import org.joda.time.LocalDateTime;
import play.data.validation.Required;

/**
 * Eventi relativi al flusso di approvazione delle competenze mensili.
 *
 * @author Dario Tagliaferri
 */
@Audited
@Builder
@ToString
@Entity
@Table(name = "competence_request_events")
public class CompetenceRequestEvent extends BaseModel {

  private static final long serialVersionUID = 7592753994383456988L;

  @NotNull
  @Column(name = "created_at")
  public LocalDateTime createdAt;
  
  @Required
  @NotNull  
  @ManyToOne(optional = false)
  @JoinColumn(name = "competence_request_id")
  public CompetenceRequest competenceRequest;
  
  @Required
  @NotNull
  @ManyToOne
  public User owner;
  
  public String description;
  
  @Required
  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "event_type")
  public CompetenceRequestEventType eventType;

  @PrePersist
  private void onUpdate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
  }
}
