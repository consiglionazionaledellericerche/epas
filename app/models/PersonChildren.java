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

import helpers.validators.LocalDatePast;
import java.time.LocalDateTime;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;
import models.base.BaseModel;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;
import play.data.validation.CheckWith;
import play.data.validation.Required;

/**
 * Questa classe è in relazione con la classe delle persone e serve a tenere traccia
 * dei figli dei dipendenti per poter verificare se è possibile, per il dipendente in
 * questione, usufruire dei giorni di permesso per malattia dei figli che sono limitati nel
 * tempo e per l'età del figlio.
 *
 * @author Dario Tagliaferri
 */
@Getter
@Setter
@Entity
@Audited
public class PersonChildren extends BaseModel {

  private static final long serialVersionUID = 2528486222814596830L;

  @Required
  private String name;

  @Required
  private String surname;

  @CheckWith(LocalDatePast.class)
  @Required
  private LocalDate bornDate;
  
  private String taxCode;

  @ManyToOne(fetch = FetchType.LAZY)
  private Person person;
  
  private String externalId;

  @NotAudited
  private LocalDateTime updatedAt;

  @PrePersist
  @PreUpdate
  private void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }
}
