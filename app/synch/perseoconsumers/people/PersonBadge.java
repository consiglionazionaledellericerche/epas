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

package synch.perseoconsumers.people;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * PersonBadge.
 *
 * @author Daniele Murgia
 * @since 28/01/19
 */
@Getter
@Setter
@Builder
public class PersonBadge {

  private Long personId;
  private String badge;
}
