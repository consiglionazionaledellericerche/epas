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

package models.enumerate;

/**
 * Ruoli di sistema.
 *
 * @author Daniele Murgia
 * @since 30/08/16
 */
public enum AccountRole {
  DEVELOPER,
  ADMIN,
  MISSIONS_MANAGER,
  CONTRACTUAL_MANAGER,
  //Amministratore in sola lettura
  RO_ADMIN, 
  ABSENCES_MANAGER,
  //Può leggere i dati di tutto il personale relativi
  //a presenze e assenze del personale
  PERSON_DAYS_READER,
  //Gestore dell'anagrafica del personale
  REGISTRY_MANAGER
}
