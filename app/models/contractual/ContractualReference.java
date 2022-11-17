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

package models.contractual;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.PreRemove;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import models.base.PeriodModel;
import org.hibernate.envers.Audited;
import com.google.common.collect.Lists;
import play.data.validation.Required;
import play.data.validation.URL;
import play.db.jpa.Blob;

/**
 * Allegato o indirizzo web di documento amministrativo.
 *
 * @author Cristian Lucchesi
 * @author Dario Tagliaferri
 */
@Getter
@Setter
@Audited
@Entity
@Table(name = "contractual_references")
public class ContractualReference extends PeriodModel {
  
  private static final long serialVersionUID = 53012052329220325L;

  @NotNull
  @Required
  private String name;

  @URL
  private String url;
  
  private String filename;

  private Blob file;

  @ManyToMany(mappedBy = "contractualReferences")
  private List<ContractualClause> contractualClauses = Lists.newArrayList();
  
  @Transient
  public long getLength() {
    return file == null ? 0 : file.length();
  }

  @PreRemove
  private void onDelete() {
    if (file != null && file.getFile() != null) {
      file.getFile().delete();  
    }    
  }
  
  @Override
  public String toString() {
    return name;
  }
}
