/*

 * Copyright (C) 2022  Consiglio Nazionale delle Ricerche
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

package cnr.sync.dto.v3;

import common.injection.StaticInject;
import lombok.Data;
import models.enumerate.BlockType;
import org.joda.time.LocalDate;
import play.data.validation.Required;

/**
 * DTO per creare via JSON le informazioni principali di un
 * blocchetto di buoni pasto.
 *
 * @author Loredana Sideri
 *
 */
@StaticInject
@Data
public class BlockMealTicketCreateDto {

  @Required
  private String codeBlock;
  @Required
  private BlockType blockType;
  @Required
  private Integer first;
  @Required
  private Integer last;
  @Required
  private LocalDate expiredDate;
  @Required
  private LocalDate deliveryDate;

  private Long contractId;
  private Long personId;
  @Required
  private Long adminId;
  private String fullname;
  private String fiscalCode;
  private String email;
  private String number; //Matricola
  private String eppn;
  private Long personPerseoId;

}