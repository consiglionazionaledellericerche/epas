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

package models.base;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitJoinTableNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;

/**
 * Clone di quello spring-boot.
 *
 * @author Marco Andreini
 *
 */
public class OurImplicitNamingStrategy extends ImplicitNamingStrategyJpaCompliantImpl {

  private static final long serialVersionUID = -721875022035961973L;

  @Override
  public Identifier determineJoinTableName(ImplicitJoinTableNameSource source) {
    String name = source.getOwningPhysicalTableName() + "_"
        + source.getAssociationOwningAttributePath().getProperty();
    return toIdentifier(name, source.getBuildingContext());
  }
}
