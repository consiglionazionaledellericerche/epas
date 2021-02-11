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

package manager.services.telework.errors;

/**
 * Tipologie di possibile errori nella gestione delle timbrature per telelavoro.
 */
public enum TeleworkStampingError {
  MEAL_STAMPING_PRESENT,
  MEAL_STAMPING_OUT_OF_BOUNDS,
  BEGIN_STAMPING_PRESENT,
  BEGIN_STAMPING_AFTER_END,
  END_STAMPING_PRESENT,
  END_STAMPING_BEFORE_BEGIN,
  INTERRUPTION_OUT_OF_BOUNDS,
  INTERRUPTION_IN_MEAL_TIME,
  EXISTING_STAMPING_BEFORE_BEGIN,
  EXISTING_STAMPING_AFTER_END,
  EXISTING_BEGIN_STAMPING_AFTER_END_MEAL,
  EXISTING_END_STAMPING_BEFORE_BEGIN_MEAL;
}
