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

package models;

import com.google.common.collect.Lists;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import models.absences.AbsenceType;
import models.base.BaseModel;
import models.base.MutableModel;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDateTime;
import play.data.validation.Required;
import play.data.validation.Unique;


/**
 * Token ricevuti dagli IDP OAuth.
 *
 * @author Cristian Lucchesi
 */
@ToString
@Getter
@Setter
@Entity
@Table(name = "jwt_tokens")
public class JwtToken extends MutableModel {

  @Unique
  @Required
  private String idToken;

  @Required
  private String accessToken;

  @Required
  private String refreshToken;

  public String tokenType;
  //Secondi
  public int expiresIn;
  public String scope;
  public LocalDateTime takenAt = LocalDateTime.now();

  public LocalDateTime expiresAt;

  @PrePersist 
  @PreUpdate
  void calculateExpiresAt() {
    if (expiresAt == null && takenAt != null) {
      expiresAt = takenAt.plusSeconds(expiresIn);
    }
  }

  public boolean isExpired() {
    return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
  }

  public boolean isExpiringSoon() {
    return expiresAt != null && expiresAt.minusMinutes(1).isBefore(LocalDateTime.now());
  }
}