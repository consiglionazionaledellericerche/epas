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

package helpers.jpa;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import models.base.BaseModel;
import play.data.binding.TypeBinder;
import play.db.jpa.JPA;

/**
 * Binder per le collezioni di oggetti JPA.
 *
 * @author Marco Andreini
 *
 */
public class JpaReferenceBinder implements TypeBinder<Collection<? extends BaseModel>> {

  @SuppressWarnings("unchecked")
  @Override
  public Object bind(String name, Annotation[] annotations, String value,
      @SuppressWarnings("rawtypes") Class actualClass, Type genericType) throws Exception {
    final Long id = Long.parseLong(value);
    return JPA.em().find(actualClass, id);
  }
}
