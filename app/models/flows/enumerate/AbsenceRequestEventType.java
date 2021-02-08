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

package models.flows.enumerate;

/**
 * Tipologie di eventi sulle richieste di assenza.
 *
 * @author Cristian Lucchesi
 *
 */
public enum AbsenceRequestEventType {
  STARTING_APPROVAL_FLOW,
  MANAGER_APPROVAL,
  MANAGER_REFUSAL,
  ADMINISTRATIVE_APPROVAL,
  ADMINISTRATIVE_REFUSAL,
  OFFICE_HEAD_APPROVAL,
  OFFICE_HEAD_REFUSAL,
  COMPLETE,
  EPAS_REFUSAL,
  DELETE,
  EXPIRING;
}
