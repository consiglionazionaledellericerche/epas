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

package controllers;

import com.google.common.collect.Lists;
import dao.history.ContractHistoryDao;
import dao.history.HistoricalDao;
import dao.history.HistoryValue;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.HistoricalManager;
import models.Competence;
import models.Contract;
import models.enumerate.HistorycalType;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Controller per visualizzazione storico competence e contratti.
 */
@Slf4j
@With({Resecure.class})
public class Historicals extends Controller {
  
  @Inject
  static ContractHistoryDao contractHistoryDao;
  @Inject
  static HistoricalDao historicalDao;
  @Inject
  static HistoricalManager historicalManager;

  /**
   * Ritorna lo storico sulle modifiche ad una competenza.
   *
   * @param competenceId l'id della competenza di cui cercare lo storico
   */
  public static void competenceHistory(long competenceId) {
    boolean found = false;
    final Competence competence = Competence.findById(competenceId);
    if (competence == null) {

      render(found);
    }
    found = true;
    List<HistoryValue<Competence>> historyCompetence = contractHistoryDao
        .competences(competenceId);
    
    render(historyCompetence, competence, found);
  }
  
  /**
   * Ritorna lo storico del contratto filtrato per tipologia: contratto/inizializzazione.
   *
   * @param contractId l'id del contratto di cui cercare lo storico
   * @param type la tipologia di informazioni che si richiedono: contratto/inizializzazione
   */
  public static void contractHistory(long contractId, HistorycalType type) {
    boolean found = false;
    final Contract contract = Contract.findById(contractId);
    if (contract == null) {
      render(found);
      return;
    }
    found = true;
    List<HistoryValue<Contract>> historyContract = contractHistoryDao.contracts(contractId);
    List<HistoryValue<Contract>> contractModifications = Lists.newArrayList();
    List<HistoryValue<Contract>> initializationModifications = Lists.newArrayList();
    Contract temp = contract;
    if (type.equals(HistorycalType.CONTRACT)) {
      for (HistoryValue<Contract> story : historyContract) {
        
        Contract con = story.value;
        log.debug("Contract id = {}, revision = {}, perseoId = {}, "
            + "beginDate = {}, endDate = {}, endContract = {}", 
            contract.id, story.revision.id, con.getPerseoId(), con.getBeginDate(), con.getEndDate(),
            con.getEndContract());  
        if (con.getBeginDate() == null && con.getEndDate() == null 
            && con.getEndContract() == null) {
          log.debug("Storico con dati incompleti per {}", contract.getPerson().getFullname());
          continue;
        }
        if (!historicalManager.checkDates(temp.getBeginDate(), con.getBeginDate())
            || !historicalManager.checkDates(temp.getEndDate(), con.getEndDate())
            || !historicalManager.checkDates(temp.getEndContract(), con.getEndContract()) 
            || temp.isOnCertificate() != con.isOnCertificate()
            || !historicalManager.checkObjects(temp, con)) {
          contractModifications.add(story);
        }
        temp = con;
      }
      render(contractModifications, contract, found);
    } else {
      for (HistoryValue<Contract> story : historyContract) {
        Contract con = story.value;
        if (!historicalManager.checkDates(temp.getSourceDateResidual(), con.getSourceDateResidual())
            || temp.getSourceRemainingMinutesLastYear() != con.getSourceRemainingMinutesLastYear()
            || temp.getSourceRemainingMinutesCurrentYear() 
                  != con.getSourceRemainingMinutesCurrentYear()) {
          
          initializationModifications.add(story);
        }
        temp = con;
      }
      render("@initializationHistory", initializationModifications, contract, found);
    }    
    
  }
}
