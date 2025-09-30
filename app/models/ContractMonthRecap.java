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

import com.google.common.base.Optional;
import dao.wrapper.IWrapperContract;
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import models.base.BaseModel;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;
import play.data.validation.Required;

/**
 * Riepilogo mensile di un contratto.
 */
@Getter
@Setter
@Entity
@Table(
    name = "contract_month_recap",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"year", "month", "contract_id"})})
@Audited
public class ContractMonthRecap extends BaseModel {

  private static final long serialVersionUID = 5381901476391668672L;

  @Required
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "contract_id")
  private Contract contract;

  @Column
  private int year;

  @Column
  private int month;

  //***************************************************************************/
  // MODULO RECAP ASSENZE
  // **************************************************************************/

  @Column(name = "abs_rc_usati")
  private Integer recoveryDayUsed = 0;        //numeroRiposiCompensativi

  //***************************************************************************/
  // * FONTI DELL'ALGORITMO RESIDUI
  // **************************************************************************/

  @Column(name = "s_r_bp_init")
  private int buoniPastoDaInizializzazione = 0;

  @Column(name = "s_r_bp")
  private int buoniPastoDalMesePrecedente = 0;

  @Column(name = "s_bp_consegnati")
  private int buoniPastoConsegnatiNelMese = 0;

  @Getter
  @Column(name = "s_bd_usati")
  private int buoniPastoUsatiNelMese = 0;

  @Column(name = "s_r_ac_initmese")
  private int initResiduoAnnoCorrenteNelMese = 0;   //per il template (se sourceContract è del mese)

  @Column(name = "s_r_ap")
  private int initMonteOreAnnoPassato = 0;        //dal precedente recap ma è utile salvarlo

  @Column(name = "s_r_ac")
  private int initMonteOreAnnoCorrente = 0;    //dal precedente recap ma è utile salvarlo

  @Column(name = "s_pf")
  private int progressivoFinaleMese = 0;            //person day

  /**
   * Questo campo ha due scopi: <br> 1) Il progressivo finale positivo da visualizzare nel template.
   * <br> 2) Il tempo disponibile per straordinari. <br> TODO: Siccome i due valori potrebbero
   * differire (esempio turnisti), decidere se splittarli in due campi distinti.
   */
  @Column(name = "s_pfp")
  private int progressivoFinalePositivoMese = 0;
  
  @Column(name = "s_pfps")
  private int progressivoFinalePositivoPerStraordinari = 0;


  @Column(name = "s_r_ap_usabile")
  private boolean possibileUtilizzareResiduoAnnoPrecedente = true;

  @Column(name = "s_s1")
  private int straordinariMinutiS1Print = 0;    //per il template

  @Column(name = "s_s2")
  private int straordinariMinutiS2Print = 0;    //per il template

  @Column(name = "s_s3")
  private int straordinariMinutiS3Print = 0;    //per il template

  @Column(name = "s_rc_min")
  private int riposiCompensativiMinutiPrint = 0;    //per il template
  
  @Column(name = "s_91ce_min")
  private int riposiCompensativiChiusuraEnteMinutiPrint = 0;    //per il template

  @Column(name = "s_ol")
  private int oreLavorate = 0;                // riepilogo per il template

  //***************************************************************************/
  // DECISIONI DELL'ALGORITMO
  // **************************************************************************/

  @Column(name = "d_pfn_ap")
  private int progressivoFinaleNegativoMeseImputatoAnnoPassato = 0;
  @Column(name = "d_pfn_ac")
  private int progressivoFinaleNegativoMeseImputatoAnnoCorrente = 0;
  @Column(name = "d_pfn_pfp")
  private int progressivoFinaleNegativoMeseImputatoProgressivoFinalePositivoMese = 0;

  @Column(name = "d_rc_ap")
  private int riposiCompensativiMinutiImputatoAnnoPassato = 0;
  @Column(name = "d_rc_ac")
  private int riposiCompensativiMinutiImputatoAnnoCorrente = 0;
  @Column(name = "d_rc_pfp")
  private int riposiCompensativiMinutiImputatoProgressivoFinalePositivoMese = 0;
  
  @Column(name = "d_91ce_ap")
  private int riposiCompensativiChiusuraEnteMinutiImputatoAnnoPassato = 0;
  @Column(name = "d_91ce_ac")
  private int riposiCompensativiChiusuraEnteMinutiImputatoAnnoCorrente = 0;
  @Column(name = "d_91ce_pfp")
  private int riposiCompensativiChiusuraEnteMinutiImputatoProgressivoFinalePositivoMese = 0;

  
  @Column(name = "d_r_ap")
  private Integer remainingMinutesLastYear = 0;

  @Column(name = "d_r_ac")
  private Integer remainingMinutesCurrentYear = 0;

  @Column(name = "d_r_bp")
  private Integer remainingMealTickets = 0; //buoniPastoResidui


  //***************************************************************************/
  // DI SUPPORTO (VALORIZZATI PER POI ESSERE IMPUTATI)
  // **************************************************************************/

  @Transient
  private int straordinariMinuti = 0;    //competences (di appoggio deducibile dalle imputazioni)

  @Transient
  private int riposiCompensativiMinuti = 0;    //absences (di appoggio deducibile dalle imputazioni)
  // in charts è usato... capire cosa contiene alla fine e fixare
  
  @Transient
  private int riposiCompensativiChiusuraEnteMinuti = 0;

  //person day  // (di appoggio deducibile dalle imputazioni)
  @Transient
  private int progressivoFinaleNegativoMese = 0;

  //**************************************************************************
  // DI SUPPORTO (VALORIZZATI PER POI ESSERE SCORPORATI)
  // ************************************************************************/

  @Transient
  public int progressivoFinalePositivoMeseAux = 0;    //person day
  // forse è usato... capire cosa contiene alla fine e fixare
  
  @Transient
  public int progressivoFinalePositivoPerStraordinariAux = 0;

  //**************************************************************************
  // TRANSIENTI DA METTERE NEL WRAPPER
  //*************************************************************************/

  @Transient
  public Person person;
  @Transient
  public Optional<ContractMonthRecap> mesePrecedente;
  @Transient
  public int qualifica;
  @Transient
  public IWrapperContract wrContract;

  @Transient
  public int getStraordinarioMinuti() {
    return this.straordinariMinutiS1Print + this.straordinariMinutiS2Print
            + this.straordinariMinutiS3Print;
  }

  /**
   * Stringa di descrizione del contratto. 
   */
  @Transient
  public String getContractDescription() {
    LocalDate beginMonth = new LocalDate(this.year, this.month, 1);
    LocalDate endMonth = beginMonth.dayOfMonth().withMaximumValue();
    DateInterval monthInterval = new DateInterval(beginMonth, endMonth);
    LocalDate endContract = this.contract.getEndDate();
    if (this.contract.getEndContract() != null) {
      endContract = this.contract.getEndContract();
    }
    if (DateUtility.isDateIntoInterval(endContract, monthInterval)) {
      return "(contratto scaduto in data " + endContract + ")";
    }
    return "";
  }

  @Transient
  public int getPositiveResidualInMonth() {

    return this.progressivoFinalePositivoMese;
  }

  /**
   * Verifica se è l'ultimo mese prima della scadenza del contratto.
   */
  @Transient
  public boolean expireInMonth() {
    if (this.contract.getEndDate() != null 
        && this.contract.getEndDate().isBefore(
            new LocalDate(year, month, 1).dayOfMonth().withMaximumValue())) {
      return true;
    }
    return false;
  }
  
  /**
   * Clean dell'oggetto persistito pre ricomputazione.
   */
  public void clean() {
    //MODULO RECAP ASSENZE

    this.recoveryDayUsed = 0;        //numeroRiposiCompensativi

    //FONTI DELL'ALGORITMO RESIDUI

    this.buoniPastoDaInizializzazione = 0;
    this.buoniPastoDalMesePrecedente = 0;
    this.buoniPastoConsegnatiNelMese = 0;
    this.buoniPastoUsatiNelMese = 0;
    this.initResiduoAnnoCorrenteNelMese = 0;
    this.initMonteOreAnnoPassato = 0;
    this.initMonteOreAnnoCorrente = 0;
    this.progressivoFinaleMese = 0;
    this.progressivoFinalePositivoMese = 0;
    this.possibileUtilizzareResiduoAnnoPrecedente = true;
    this.straordinariMinutiS1Print = 0;
    this.straordinariMinutiS2Print = 0;
    this.straordinariMinutiS3Print = 0;
    this.riposiCompensativiMinutiPrint = 0;
    this.oreLavorate = 0;

    //DECISIONI DELL'ALGORITMO

    this.progressivoFinaleNegativoMeseImputatoAnnoPassato = 0;
    this.progressivoFinaleNegativoMeseImputatoAnnoCorrente = 0;
    this.progressivoFinaleNegativoMeseImputatoProgressivoFinalePositivoMese = 0;
    this.riposiCompensativiMinutiImputatoAnnoPassato = 0;
    this.riposiCompensativiMinutiImputatoAnnoCorrente = 0;
    this.riposiCompensativiMinutiImputatoProgressivoFinalePositivoMese = 0;
    this.riposiCompensativiChiusuraEnteMinutiImputatoAnnoCorrente = 0;
    this.riposiCompensativiChiusuraEnteMinutiImputatoAnnoPassato = 0;
    this.riposiCompensativiChiusuraEnteMinutiImputatoProgressivoFinalePositivoMese = 0;
    this.remainingMinutesLastYear = 0;
    this.remainingMinutesCurrentYear = 0;
    this.remainingMealTickets = 0;

  }

}
