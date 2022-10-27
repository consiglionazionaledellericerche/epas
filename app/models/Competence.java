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

import java.math.BigDecimal;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import models.base.BaseModel;
import org.hibernate.envers.Audited;
import play.data.validation.Required;


/**
 * Tabella delle competenze relative alla persona in cui sono memorizzate le competenze in
 * determinate date (espresse attraverso due interi, uno relativo all'anno e uno relativo al mese
 * con relative descrizioni e valori.
 *
 * @author Dario Tagliaferri
 * @author Arianna Del Soldato
 */
@Getter
@Setter
@Entity
@Table(name = "competences")
@Audited
public class Competence extends BaseModel {

  private static final long serialVersionUID = -36737525666037452L;

  @ManyToOne
  private Person person;

  @NotNull
  @Required
  @ManyToOne
  private CompetenceCode competenceCode;

  private int year;

  private int month;

  private BigDecimal valueRequested = BigDecimal.ZERO;

  private Integer exceededMins;

  private int valueApproved;


  private String reason;


  /**
   * Costruttore.
   *
   * @param person la persona
   * @param competenceCode il codice di competenza
   * @param year l'anno
   * @param month il mese
   */
  public Competence(
      Person person, CompetenceCode competenceCode, int year, int month) {
    this.person = person;
    this.competenceCode = competenceCode;
    this.year = year;
    this.month = month;
  }

  /**
   * Costruttore.
   *
   * @param person la persona
   * @param competenceCode il codice di competenza
   * @param year l'anno
   * @param month il mese
   * @param value la quantità
   * @param reason la motivazione
   */
  public Competence(
      Person person, CompetenceCode competenceCode, int year, int month, int value, String reason) {
    this.person = person;
    this.competenceCode = competenceCode;
    this.year = year;
    this.month = month;
    valueApproved = value;
    this.reason = reason;
  }

  /**
   * Costruttore vuoto.
   */
  public Competence() {
  }

  @Override
  public String toString() {
    return String.format("Competenza %s nel mese %s-%s per %s: %s", competenceCode, month, year,
        person, valueApproved);
  }

}
