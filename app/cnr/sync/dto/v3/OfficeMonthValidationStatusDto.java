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

package cnr.sync.dto.v3;

import cnr.sync.dto.v2.PersonShowTerseDto;
import java.util.List;
import lombok.Data;
import org.testng.collections.Lists;

/**
 * DTO per contenere le informazioni di validazione degli attestati
 * validati su Attestati.
 *
 * @author Cristian Lucchesi
 */
@Data
public class OfficeMonthValidationStatusDto {

  private List<PersonShowTerseDto> validatedPersons = Lists.newArrayList();
  private List<PersonShowTerseDto> notValidatedPersons = Lists.newArrayList();

  /**
   * Controlla se tutti gli attestati sono stati validati.
   *
   * @return true se tutti gli attestati sono stati validati, false altrimenti.
   */
  public boolean isAllCertificationsValidated() {
    return !validatedPersons.isEmpty() && notValidatedPersons.isEmpty();
  }
}