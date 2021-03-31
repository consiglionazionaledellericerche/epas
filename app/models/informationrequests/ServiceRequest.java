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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import org.hibernate.envers.Audited;
import models.base.InformationRequest;
import play.data.validation.Required;

@Audited
@Entity
@Table(name = "service_requests")
//@DiscriminatorValue("serviceInformation")
@PrimaryKeyJoinColumn(name = "informationRequestId")
public class ServiceRequest extends InformationRequest{

  @Required
  @NotNull
  public LocalDate day;
  @Required
  @NotNull
  public LocalTime beginAt;
  @Required
  @NotNull
  public LocalTime finishTo;
  @Required
  @NotNull
  public String reason;
  
  /**
   * Orario formattato come HH:mm.
   *
   * @return orario della timbratura formattato come HH:mm.
   */
  @Transient
  public String formattedHour(LocalTime time) {
    if (time != null) {
      return time.format(DateTimeFormatter.ISO_LOCAL_TIME);
    } else {
      return "";
    }
  }
}
