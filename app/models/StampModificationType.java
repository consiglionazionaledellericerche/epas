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

package models;

import java.util.List;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import models.base.BaseModel;
import net.sf.oval.constraint.MinLength;
import org.hibernate.envers.Audited;
import play.data.validation.Required;

/**
 * Tipo di modifica alla timbratura.
 */
@Getter
@Setter
@Audited
@Entity
@Table(name = "stamp_modification_types")
public class StampModificationType extends BaseModel {

  private static final long serialVersionUID = 8403725731267832733L;

  @Required
  private String code;

  @Required
  @MinLength(value = 2)
  private String description;

  @OneToMany(mappedBy = "stampModificationType")
  private Set<Stamping> stampings;

  @OneToMany(mappedBy = "stampModificationType")
  private List<PersonDay> personDays;

}
