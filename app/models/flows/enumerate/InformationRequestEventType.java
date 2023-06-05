/*
 * Copyright (C) 2023  Consiglio Nazionale delle Ricerche
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
 * Enumerato per le tipologie di stato nel flusso di approvazione delle richieste
 * di flusso informativo.
 *
 * @author Dario Tagliaferri
 *
 */
public enum InformationRequestEventType {

  STARTING_APPROVAL_FLOW,
  OFFICE_HEAD_ACKNOWLEDGMENT,
  OFFICE_HEAD_REFUSAL,
  ADMINISTRATIVE_ACKNOWLEDGMENT,
  ADMINISTRATIVE_REFUSAL,
  MANAGER_ACKNOWLEDGMENT,
  MANAGER_REFUSAL,
  DELETE,
  EPAS_REFUSAL,
  COMPLETE;
}
