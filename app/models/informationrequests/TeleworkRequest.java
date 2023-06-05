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

package models.informationrequests;

import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import models.base.InformationRequest;
import org.hibernate.envers.Audited;
import play.data.validation.Required;

/**
 * classe relativa alla richiesta di telelavoro.
 *
 * @author dario
 *
 */
@Getter
@Setter
@Audited
@Entity
@Table(name = "telework_requests")
@PrimaryKeyJoinColumn(name = "informationRequestId")
public class TeleworkRequest extends InformationRequest {

  private static final long serialVersionUID = -2647537080585976423L;

  @Required
  @NotNull
  private int month;
  @Required
  @NotNull
  private int year;

  private String context;
}
