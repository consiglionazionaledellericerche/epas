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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PreRemove;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import models.base.MutableModel;
import models.enumerate.AttachmentType;
import org.hibernate.envers.Audited;
import play.data.validation.Required;
import play.db.jpa.Blob;

/**
 * Oggetto che modella gli allegati.
 *
 * @author Daniele Murgia
 * @since 06/10/16
 */
@Getter
@Setter
@Audited
@Entity
@Table(name = "attachments")
public class Attachment extends MutableModel {

  private static final long serialVersionUID = 7907525510585924187L;

  @NotNull
  @Required
  private String filename;

  private String description;

  @NotNull
  @Enumerated(EnumType.STRING)
  private AttachmentType type;

  @NotNull
  @Column(nullable = false)
  private Blob file;

  @ManyToOne(optional = true)
  @JoinColumn(name = "office_id")
  private Office office;

  /**
   * Dimensione dell'allegato.
   */
  @Transient
  public long getLength() {
    return file == null ? 0 : file.length();
  }

  @PreRemove
  private void onDelete() {
    this.file.getFile().delete();
  }
}
