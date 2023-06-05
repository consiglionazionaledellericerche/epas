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

import java.time.LocalDate;
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
 * Classe di richieste informative per malattia.
 *
 * @author dario
 *
 */
@Getter
@Setter
@Audited
@Entity
@Table(name = "illness_requests")
@PrimaryKeyJoinColumn(name = "informationRequestId")
public class IllnessRequest extends InformationRequest {

  private static final long serialVersionUID = -6222751376445648447L;

  @Required
  @NotNull
  private LocalDate beginDate;
  @Required
  @NotNull
  private LocalDate endDate;
  @Required
  @NotNull
  private String name;
}
