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

package manager;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import dao.AbsenceDao;
import dao.CompetenceDao;
import dao.MealTicketDao;
import dao.PersonDayDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import manager.cache.CompetenceCodeManager;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import manager.services.mealtickets.MealTicketsServiceImpl.MealTicketOrder;
import models.Competence;
import models.CompetenceCode;
import models.Contract;
import models.ContractMonthRecap;
import models.ContractWorkingTimeType;
import models.PersonDay;
import models.TimeVariation;
import models.WorkingTimeTypeDay;
import models.absences.Absence;
import org.assertj.core.util.Sets;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

/**
 * Contentitore per le funzionalità relative ai ContractMonthRecap.
 *
 * @author Alessandro Martelli
 */
public class ContractMonthRecapManager {

  @Inject
  private MealTicketDao mealTicketDao;
  @Inject
  private PersonDayDao personDayDao;
  @Inject
  private CompetenceDao competenceDao;
  @Inject
  private CompetenceCodeManager competenceCodeManager;
  @Inject
  private AbsenceDao absenceDao;
  @Inject
  private IWrapperFactory wrapperFactory;
  @Inject
  private ConfigurationManager configurationManager;

  /**
   * Nota bene: il metodo non effettua salvataggi ma solo assegnamenti.<br> Nota bene 2: il metodo
   * assume che se il precedente riepilogo è absent() significa che non serve ai fini della
   * computazione. Il controllo di fornire il parametro corretto è demandato al chiamante. <br>
   *
   * <p>Aggiorna i campi del riepilogo contenuto in cmr, nella parte residuale ovvero:<br> - residui
   * anno corrente <br> - residui anno passato <br> - buoni pasto rimanenti <br> alla data
   * calcolaFinoA.<br>
   * </p>
   * 
   * <p>Durante la computazione memorizza anche le seguenti informazioni in cmr: <br> - progressivo
   * finale totale, positivo e negativo del mese <br> - straordinari s1, s2, s3 assegnati nel
   * mese<br> - riposi compensativi effettuati in numero e minuti totali<br> - buoni pasto
   * consegnati ed consumati nel mese <br>
   * </p>
   *
   * @param recapPreviousMonth    se presente è il riepilogo precedente.
   * @param otherCompensatoryRest altri riposi compensativi non persistiti nel db.
   * @param compensatoryRestClosureSeatRecovering variazioni orarie per recuperare i riposi 
   *      compensativi per chiusura ente.
   */
  public Optional<ContractMonthRecap> computeResidualModule(ContractMonthRecap cmr,
      Optional<ContractMonthRecap> recapPreviousMonth, YearMonth yearMonth, LocalDate calcolaFinoA,
      List<Absence> otherCompensatoryRest, 
      Optional<List<TimeVariation>> compensatoryRestClosureSeatRecovering) {

    Contract contract = cmr.getContract();

    //TODO: controllare che in otherCompensatoryRest ci siano solo riposi compensativi.

    //////////////////////////////////////////////////////////////////////////
    //Valori dei minuti residui all'inizio del mese richiesto
    // Se è il mese di beginContrat i residui sono 0
    int initMonteOreAnnoPassato = 0;
    int initMonteOreAnnoCorrente = 0;
    if (recapPreviousMonth.isPresent()) {
      //Se ho il riepilogo del mese precedente lo utilizzo.
      if (recapPreviousMonth.get().getMonth() == 12) {
        initMonteOreAnnoPassato =
            recapPreviousMonth.get().getRemainingMinutesCurrentYear()
                + recapPreviousMonth.get().getRemainingMinutesLastYear();

      } else {
        initMonteOreAnnoCorrente = recapPreviousMonth.get().getRemainingMinutesCurrentYear();
        initMonteOreAnnoPassato = recapPreviousMonth.get().getRemainingMinutesLastYear();
      }
    }
    if (contract.getSourceDateResidual() != null
        && contract.getSourceDateResidual().getYear() == yearMonth.getYear()
        && contract.getSourceDateResidual().getMonthOfYear() == yearMonth.getMonthOfYear()) {
      //Se è il primo riepilogo dovuto ad inzializzazione utilizzo i dati
      //in source
      initMonteOreAnnoPassato = contract.getSourceRemainingMinutesLastYear();
      initMonteOreAnnoCorrente = contract.getSourceRemainingMinutesCurrentYear();
    }

    //////////////////////////////////////////////////////////////////////////
    //Valori dei buoni pasto residui all'inizio del mese richiesto
    // Se è il mese di beginContrat i residui sono 0
    cmr.setBuoniPastoDaInizializzazione(0);
    if (recapPreviousMonth.isPresent()) {
      //Se ho il riepilogo del mese precedente lo utilizzo.
      cmr.setBuoniPastoDalMesePrecedente(
          recapPreviousMonth.get().getRemainingMealTickets());
    }
    //Se è il primo riepilogo dovuto ad inzializzazione utilizzo i dati
    //in source
    if (contract.getSourceDateMealTicket() != null
        && contract.getSourceDateMealTicket().getYear() == yearMonth.getYear()
        && contract.getSourceDateMealTicket().getMonthOfYear() == yearMonth.getMonthOfYear()) {
      cmr.setBuoniPastoDaInizializzazione(contract.getSourceRemainingMealTicket());
      cmr.setBuoniPastoDalMesePrecedente(0);
    }
    //Ma c'è il particolarissimo caso in cui il contratto inizia il primo del mese,
    // non ho definito inizializzazione generale, e voglio impostare il residuo iniziale 
    // (all'ultimo giorno del mese precedente)
    if (contract.getSourceDateResidual() == null 
        && contract.getSourceDateMealTicket() != null 
        && new YearMonth(contract.getBeginDate()).equals(yearMonth) 
        && contract.getBeginDate().getDayOfMonth() == 1 
        && contract.getSourceDateMealTicket().isEqual(contract.getBeginDate().minusDays(1))) {
      
      cmr.setBuoniPastoDaInizializzazione(0);
      cmr.setBuoniPastoDalMesePrecedente(contract.getSourceRemainingMealTicket());
    }

    //////////////////////////////////////////////////////////////////////////
    //Inizio Algoritmo
    cmr.wrContract = wrapperFactory.create(contract);
    cmr.person = contract.getPerson();
    cmr.qualifica = cmr.person.getQualification().getQualification();

    cmr.setInitMonteOreAnnoCorrente(initMonteOreAnnoCorrente);
    cmr.setInitMonteOreAnnoPassato(initMonteOreAnnoPassato);

    //Per stampare a video il residuo da inizializzazione se riferito al mese
    if (contract.getSourceDateResidual() != null
        && contract.getSourceDateResidual().getMonthOfYear() == cmr.getMonth()
        && contract.getSourceDateResidual().getYear() == cmr.getYear()) {
      cmr.setInitResiduoAnnoCorrenteNelMese(contract.getSourceRemainingMinutesCurrentYear());
    }

    //Inizializzazione residui
    //Gennaio
    if (cmr.getMonth() == 1) {
      cmr.mesePrecedente = null;
      cmr.setRemainingMinutesLastYear(initMonteOreAnnoPassato);
      cmr.setRemainingMinutesCurrentYear(initMonteOreAnnoCorrente);

      //se il residuo iniziale e' negativo lo tolgo dal residio mensile positivo
      if (cmr.getRemainingMinutesLastYear() < 0) {
        cmr.progressivoFinalePositivoMeseAux = cmr.progressivoFinalePositivoMeseAux
            + cmr.getRemainingMinutesLastYear();
        cmr.setRemainingMinutesLastYear(0);
      }
    } else {
      cmr.mesePrecedente = recapPreviousMonth;
      cmr.setRemainingMinutesLastYear(initMonteOreAnnoPassato);
      cmr.setRemainingMinutesCurrentYear(initMonteOreAnnoCorrente);

      EpasParam param = cmr.qualifica > 3 ? EpasParam.MONTH_EXPIRY_RECOVERY_DAYS_49 :
          EpasParam.MONTH_EXPIRY_RECOVERY_DAYS_13;
      Integer monthExpiryRecoveryDay = (Integer) configurationManager
          .configValue(cmr.getPerson().getOffice(), param, cmr.getYear());

      if (monthExpiryRecoveryDay != 0 && cmr.getMonth() > monthExpiryRecoveryDay) {
        cmr.setPossibileUtilizzareResiduoAnnoPrecedente(false);
        cmr.setRemainingMinutesLastYear(0);
      }
    }

    //Costruzione degli intervalli e recupero delle informazioni dal db.
    DateInterval validDataForPersonDay = buildIntervalForProgressive(yearMonth,
        calcolaFinoA, contract);

    DateInterval validDataForCompensatoryRest =
        buildIntervalForCompensatoryRest(yearMonth, contract);

    DateInterval validDataForMealTickets = buildIntervalForMealTicket(yearMonth,
        contract);

    IWrapperContract wrContract = wrapperFactory.create(cmr.getContract());

    setMealTicketsInformation(cmr, validDataForMealTickets);
    setPersonDayInformation(cmr, validDataForPersonDay, otherCompensatoryRest);
    setPersonMonthInformation(cmr, wrContract,
        validDataForCompensatoryRest, otherCompensatoryRest, compensatoryRestClosureSeatRecovering);

    //Imputazioni
    assegnaProgressivoFinaleNegativo(cmr);
    assegnaStraordinari(cmr);
    assegnaRiposiCompensativi(cmr);
    assegnaRiposiCompensativiChiusuraEnte(cmr);

    //Correzioni alle imputazioni

    //1) All'anno corrente imputo:
    //sia ciò che ho imputato al residuo del mese precedente dell'anno corrente
    //sia ciò che ho imputato al progressivo finale positivo del mese
    //perchè non ho interesse a visualizzarli separati nel template.
    cmr.setProgressivoFinaleNegativoMeseImputatoAnnoCorrente(cmr
        .getProgressivoFinaleNegativoMeseImputatoAnnoCorrente()
            + cmr.getProgressivoFinaleNegativoMeseImputatoProgressivoFinalePositivoMese());
    cmr.setRiposiCompensativiMinutiImputatoAnnoCorrente(cmr
        .getRiposiCompensativiMinutiImputatoAnnoCorrente()
            + cmr.getRiposiCompensativiMinutiImputatoProgressivoFinalePositivoMese());

    //2) Al monte ore dell'anno corrente aggiungo:
    // ciò che non ho utilizzato del progressivo finale positivo del mese
    cmr.setRemainingMinutesCurrentYear(cmr.getRemainingMinutesCurrentYear()
        + cmr.progressivoFinalePositivoMeseAux);

    return Optional.fromNullable(cmr);
  }

  /**
   * Costruisce l'intervallo dei giorni da considerare per il calcolo dei progressivi.<br> 1) Parto
   * dall'intero intervallo del mese.<br> 2) Nel caso di calcolo riepilogo mese attuale considero i
   * giorni fino a ieri. (se oggi è il primo giorno del mese ritorna l'intervallo vuoto)<br> 3)
   * Riduco ulteriormente in base al parametro calcolaFinoA e sull'intervallo del contratto nel
   * database.
   */
  private DateInterval buildIntervalForProgressive(YearMonth yearMonth,
      LocalDate calcolaFinoA, Contract contract) {

    LocalDate firstDayOfRequestedMonth =
        new LocalDate(yearMonth.getYear(), yearMonth.getMonthOfYear(), 1);
    DateInterval requestInterval =
        new DateInterval(firstDayOfRequestedMonth, calcolaFinoA);

    DateInterval contractDatabaseInterval =
        wrapperFactory.create(contract).getContractDatabaseInterval();

    LocalDate today = LocalDate.now();

    //Parto da tutti i giorni del mese
    LocalDate monthBegin = new LocalDate(yearMonth.getYear(), yearMonth.getMonthOfYear(), 1);
    LocalDate monthEnd = monthBegin.dayOfMonth().withMaximumValue();
    DateInterval monthInterval = new DateInterval(monthBegin, monthEnd);

    //Filtro se mese attuale
    if (DateUtility.isDateIntoInterval(today, monthInterval)) {

      if (today.getDayOfMonth() != 1) {

        //Se oggi non è il primo giorno del mese allora
        //tutti i giorni del mese fino a ieri.

        monthEnd = today.minusDays(1);
        monthInterval = new DateInterval(monthBegin, monthEnd);
      } else {

        //Se oggi è il primo giorno del mese allora nessun giorno.
        monthInterval = null;
      }
    }

    //Filtro per dati nel database e estremi del contratto
    DateInterval validDataForPersonDay = null;
    if (monthInterval != null) {
      validDataForPersonDay = DateUtility
          .intervalIntersection(monthInterval, requestInterval);
      validDataForPersonDay = DateUtility
          .intervalIntersection(validDataForPersonDay, contractDatabaseInterval);
    }

    return validDataForPersonDay;
  }

  /**
   * Costruisce l'intervallo dei giorni da considerare per il conteggio dei riposi compensativi
   * utilizzati.<br> 1) Parto dall'intero intervallo del mese.<br> 2) Nel caso di calcolo riepilogo
   * mese attuale considero tutti i giorni del mese attuale e di quello successivo<br> 3) Riduco
   * ulteriormente in base agli estremi del contratto nel database.
   */
  private DateInterval buildIntervalForCompensatoryRest(YearMonth yearMonth,
      Contract contract) {

    DateInterval contractDatabaseInterval =
        wrapperFactory.create(contract).getContractDatabaseInterval();

    LocalDate today = LocalDate.now();

    //Parto da tutti i giorni del mese
    LocalDate monthBegin = new LocalDate(yearMonth.getYear(), yearMonth.getMonthOfYear(), 1);
    LocalDate monthEnd = monthBegin.dayOfMonth().withMaximumValue();
    DateInterval monthInterval = new DateInterval(monthBegin, monthEnd);

    //Nel caso del mese attuale considero anche il mese successivo
    if (DateUtility.isDateIntoInterval(today, monthInterval)) {
      monthEnd = monthEnd.plusMonths(1).dayOfMonth().withMaximumValue();
      monthInterval = new DateInterval(monthBegin, monthEnd);
    }

    //Filtro per dati nel database e estremi del contratto
    DateInterval validDataForCompensatoryRest = null;
    validDataForCompensatoryRest = DateUtility
        .intervalIntersection(monthInterval, contractDatabaseInterval);
    return validDataForCompensatoryRest;

  }

  /**
   * Costruisce l'intervallo dei giorni da considerare per il calcolo buoni pasto utilizzati.<br> Se
   * l'office della persona non ha una data di inizio utilizzo buoni pasto ritorna un intervallo
   * vuoto (null)<br> 1) Parto dall'intero intervallo del mese.<br> 2) Nel caso di calcolo riepilogo
   * mese attuale considero i giorni fino a oggi. <br> 3) Riduco ulteriormente in base al parametro
   * calcolaFinoA, in base all'inizializzazione del contratto per buoni pasto ed alla data di inizio
   * utilizzo buoni pasto dell'office.
   */
  private DateInterval buildIntervalForMealTicket(YearMonth yearMonth, Contract contract) {

    // FIXME: nel caso di buono pasto attribuito oggi non prenderei il dato
    // in fin dei conti la restrizione sul calcolaFinoA potrebbe essere inutile
    // nel caso dei buoni pasto attribuiti. Valutare

    //LocalDate firstDayOfRequestedMonth =
    //  new LocalDate(yearMonth.getYear(),yearMonth.getMonthOfYear(),1);
    //DateInterval requestInterval = new DateInterval(firstDayOfRequestedMonth, calcolaFinoA);

    LocalDate dateStartMealTicketInOffice = (LocalDate) configurationManager
        .configValue(contract.getPerson().getOffice(), EpasParam.DATE_START_MEAL_TICKET);

    LocalDate today = LocalDate.now();

    //Parto da tutti i giorni del mese
    LocalDate monthBegin = new LocalDate(yearMonth.getYear(), yearMonth.getMonthOfYear(), 1);
    LocalDate monthEnd = monthBegin.dayOfMonth().withMaximumValue();
    DateInterval monthInterval = new DateInterval(monthBegin, monthEnd);

    //Nel caso del calcolo del mese attuale considero dall'inizio
    //del mese fino a oggi.
    if (DateUtility.isDateIntoInterval(today, monthInterval)) {
      monthEnd = today;
      monthInterval = new DateInterval(monthBegin, monthEnd);
    }

    //Filtro per dati nel database, estremi del contratto, inizio utilizzo buoni pasto
    DateInterval contractIntervalForMealTicket =
        wrapperFactory.create(contract).getContractDatabaseIntervalForMealTicket();
    DateInterval mealTicketIntervalInOffice =
        DateInterval.withBegin(dateStartMealTicketInOffice, Optional.<LocalDate>absent());

    DateInterval validDataForMealTickets = null;
    if (monthInterval != null) {
      validDataForMealTickets = DateUtility
          .intervalIntersection(monthInterval, contractIntervalForMealTicket);

      validDataForMealTickets = DateUtility
          .intervalIntersection(validDataForMealTickets, mealTicketIntervalInOffice);
    }

    return validDataForMealTickets;

  }

  /**
   * Assegna i seguenti campi del riepilogo mensile: <br> cmr.progressivoFinaleMese <br>
   * cmr.progressivoFinalePositivoMeseAux <br> cmr.progressivoFinaleNegativoMese <br>
   */
  private void setPersonDayInformation(ContractMonthRecap cmr,
      DateInterval validDataForPersonDay, List<Absence> otherCompensatoryRests) {

    if (validDataForPersonDay != null) {

      // TODO: implementare un metodo che no fa fetch di stampings... in
      // questo caso non servono.

      List<PersonDay> pdList = personDayDao.getPersonDayInPeriodDesc(
          cmr.person, validDataForPersonDay.getBegin(),
          Optional.fromNullable(validDataForPersonDay.getEnd()));

      //progressivo finale fine mese
      for (PersonDay pd : pdList) {
        if (pd != null) {
          cmr.setProgressivoFinaleMese(pd.getProgressive());
          break;
        } else {
          //
        }
      }

      //Nei giorni in cui simulo l'inserimento di riposi compensativi la differenza è zero.
      Set<LocalDate> otherCompensatoryDates = Sets.newHashSet(); 
      for (Absence otherCompensatoryRest : otherCompensatoryRests) {
        otherCompensatoryDates.add(otherCompensatoryRest.getAbsenceDate());
      }
      
      //progressivo finale positivo e negativo mese
      for (PersonDay pd : pdList) {
        if (otherCompensatoryDates.contains(pd.getDate())) {
          continue;
        }
        if (pd.getDifference() >= 0) {
          cmr.progressivoFinalePositivoMeseAux += pd.getDifference();
        } else {
          cmr.setProgressivoFinaleNegativoMese(cmr.getProgressivoFinaleNegativoMese() 
              + pd.getDifference());
        }
        cmr.setOreLavorate(cmr.getOreLavorate() + pd.getTimeAtWork());
      }
      cmr.setProgressivoFinaleNegativoMese(cmr.getProgressivoFinaleNegativoMese() * -1);

      cmr.setProgressivoFinalePositivoMese(cmr.progressivoFinalePositivoMeseAux);

    }
  }

  /**
   * Assegna i seguenti campi del riepilogo mensile: <br> cmr.buoniPastoUsatiNelMese <br>
   * cmr.buoniPastoConsegnatiNelMese <br> cmr.remainingMealTickets <br>
   *
   * @param validDataForMealTickets l'intervallo all'interno del quale ricercare i person day per
   *                                buoni pasto utilizzati e buoni pasto consegnati.
   */
  private void setMealTicketsInformation(ContractMonthRecap cmr,
      DateInterval validDataForMealTickets) {

    if (validDataForMealTickets != null) {
      List<PersonDay> pdList = personDayDao.getPersonDayInPeriod(cmr.person,
          validDataForMealTickets.getBegin(),
          Optional.fromNullable(validDataForMealTickets.getEnd()));

      //buoni pasto utilizzati
      for (PersonDay pd : pdList) {
        if (pd != null && pd.isTicketAvailable()) {
          cmr.setBuoniPastoUsatiNelMese(cmr.getBuoniPastoUsatiNelMese() + 1);
        }
      }

      //Numero ticket consegnati nel mese
      cmr.setBuoniPastoConsegnatiNelMese(mealTicketDao
          .contractMealTickets(cmr.getContract(), Optional.of(validDataForMealTickets),
              MealTicketOrder.ORDER_BY_EXPIRE_DATE_ASC, false).size());
    }

    //residuo
    cmr.setRemainingMealTickets(cmr.getBuoniPastoDalMesePrecedente()
        + cmr.getBuoniPastoDaInizializzazione()
        + cmr.getBuoniPastoConsegnatiNelMese()
        - cmr.getBuoniPastoUsatiNelMese());
  }

  /**
   * Assegna i seguenti campi del riepilogo mensile: <br> cmr.straordinariMinuti <br>
   * cmr.straordinariMinutiS1Print <br> cmr.straordinariMinutiS2Print <br>
   * cmr.straordinariMinutiS3Print <br> cmr.riposiCompensativiMinuti <br> cmr.recoveryDayUsed <br>
   *
   * @param validDataForCompensatoryRest l'intervallo all'interno del quale ricercare i riposi
   *                                     compensativi
   * @param otherCompensatoryRest        i riposi compensativi inseriti e non persistiti (usato per
   *                                     le simulazioni di inserimento assenze).
   * @param compensatoryRestClosureSeatRecovering la lista delle variazioni orarie per 
   *      recuperare i riposi compensativi per chiusura ente fatte nel mese.
   */
  private void setPersonMonthInformation(
      ContractMonthRecap cmr, IWrapperContract wrContract,
      DateInterval validDataForCompensatoryRest, List<Absence> otherCompensatoryRest,
      Optional<List<TimeVariation>> compensatoryRestClosureSeatRecovering) {

    //gli straordinari li assegno solo all'ultimo contratto attivo del mese
    if (wrContract.isLastInMonth(cmr.getMonth(), cmr.getYear())) {

      CompetenceCode s1 = competenceCodeManager.getCompetenceCode("S1");
      CompetenceCode s2 = competenceCodeManager.getCompetenceCode("S2");
      CompetenceCode s3 = competenceCodeManager.getCompetenceCode("S3");
      List<CompetenceCode> codes = Lists.newArrayList();
      codes.add(s1);
      codes.add(s2);
      codes.add(s3);

      cmr.setStraordinariMinuti(0);
      cmr.setStraordinariMinutiS1Print(0);
      cmr.setStraordinariMinutiS2Print(0);
      cmr.setStraordinariMinutiS3Print(0);

      List<Competence> competences = 
          competenceDao.getCompetences(
              Optional.fromNullable(cmr.person), cmr.getYear(), 
              Optional.fromNullable(cmr.getMonth()), codes);

      for (Competence competence : competences) {

        if (competence.getCompetenceCode().id.equals(s1.id)) {
          cmr.setStraordinariMinutiS1Print((competence.getValueApproved() * 60));
        } else if (competence.getCompetenceCode().id.equals(s2.id)) {
          cmr.setStraordinariMinutiS2Print((competence.getValueApproved() * 60));
        } else if (competence.getCompetenceCode().id.equals(s3.id)) {
          cmr.setStraordinariMinutiS3Print((competence.getValueApproved() * 60));
        }
      }

      cmr.setStraordinariMinuti(cmr.getStraordinariMinutiS1Print()
          + cmr.getStraordinariMinutiS2Print()
          + cmr.getStraordinariMinutiS3Print());
    }

    if (validDataForCompensatoryRest != null) {

      cmr.setRiposiCompensativiMinuti(0);
      cmr.setRecoveryDayUsed(0);

      for (Absence riposo : otherCompensatoryRest) {
        if (DateUtility.isDateIntoInterval(riposo.date, validDataForCompensatoryRest)) {

          // TODO: rifattorizzare questa parte. Serve un metodo
          // .getWorkingTimeTypeDay(date) in WrapperContract

          LocalDate date = riposo.date;
          for (ContractWorkingTimeType cwtt :
              wrContract.getValue().getContractWorkingTimeType()) {

            if (DateUtility.isDateIntoInterval(date,
                wrapperFactory.create(cwtt).getDateInverval())) {

              WorkingTimeTypeDay wttd = cwtt.getWorkingTimeType().getWorkingTimeTypeDays()
                  .get(date.getDayOfWeek() - 1);

              Preconditions.checkState(wttd.getDayOfWeek() == date.getDayOfWeek());
              cmr.setRiposiCompensativiMinuti(cmr.getRiposiCompensativiMinuti() 
                  + wttd.getWorkingTime());
              cmr.setRecoveryDayUsed(cmr.getRecoveryDayUsed() + 1);
            }
          }
        }
      }

      LocalDate begin = validDataForCompensatoryRest.getBegin();
      LocalDate end = validDataForCompensatoryRest.getEnd();
      List<Absence> riposi =
          absenceDao.absenceInPeriod(cmr.person, begin, end, "91");

      for (Absence abs : riposi) {
        cmr.setRiposiCompensativiMinuti(cmr.getRiposiCompensativiMinuti() 
            + wrapperFactory.create(abs.getPersonDay())
            .getWorkingTimeTypeDay().get().getWorkingTime());
        cmr.setRecoveryDayUsed(cmr.getRecoveryDayUsed() + 1);
      }

      cmr.setRiposiCompensativiMinutiPrint(cmr.getRiposiCompensativiMinuti());
      if (compensatoryRestClosureSeatRecovering.isPresent()) {
        for (TimeVariation tv : compensatoryRestClosureSeatRecovering.get()) {
          if (tv.getDateVariation().getYear() == cmr.getYear() 
              && tv.getDateVariation().getMonthOfYear() == cmr.getMonth()) {
            cmr.setRiposiCompensativiChiusuraEnteMinuti(
                cmr.getRiposiCompensativiChiusuraEnteMinuti() + tv.getTimeVariation());
          }          
        }
        cmr.setRiposiCompensativiChiusuraEnteMinutiPrint(cmr
            .getRiposiCompensativiChiusuraEnteMinuti());
      }
      
    }
  }

  private void assegnaProgressivoFinaleNegativo(ContractMonthRecap monthRecap) {

    //quello che assegno al monte ore passato
    if (monthRecap.getProgressivoFinaleNegativoMese() < monthRecap.getRemainingMinutesLastYear()) {
      monthRecap.setRemainingMinutesLastYear(
          monthRecap.getRemainingMinutesLastYear() - monthRecap.getProgressivoFinaleNegativoMese());
      monthRecap.setProgressivoFinaleNegativoMeseImputatoAnnoPassato(
          monthRecap.getProgressivoFinaleNegativoMese());
      return;
    } else {
      monthRecap.setProgressivoFinaleNegativoMeseImputatoAnnoPassato(
          monthRecap.getRemainingMinutesLastYear());
      monthRecap.setRemainingMinutesLastYear(0);
      monthRecap.setProgressivoFinaleNegativoMese(
          monthRecap.getProgressivoFinaleNegativoMese()
              -
              monthRecap.getProgressivoFinaleNegativoMeseImputatoAnnoPassato());
    }

    //quello che assegno al monte ore corrente
    if (monthRecap.getProgressivoFinaleNegativoMese() 
          < monthRecap.getRemainingMinutesCurrentYear()) {
      monthRecap.setRemainingMinutesCurrentYear(
          monthRecap.getRemainingMinutesCurrentYear() 
          - monthRecap.getProgressivoFinaleNegativoMese());
      monthRecap.setProgressivoFinaleNegativoMeseImputatoAnnoCorrente(
          monthRecap.getProgressivoFinaleNegativoMese());
      return;
    } else {
      monthRecap.setProgressivoFinaleNegativoMeseImputatoAnnoCorrente(
          monthRecap.getRemainingMinutesCurrentYear());
      monthRecap.setRemainingMinutesCurrentYear(0);
      monthRecap.setProgressivoFinaleNegativoMese(
          monthRecap.getProgressivoFinaleNegativoMese()
              -
              monthRecap.getProgressivoFinaleNegativoMeseImputatoAnnoCorrente());
    }

    //quello che assegno al progressivo positivo del mese
    monthRecap.progressivoFinalePositivoMeseAux =
        monthRecap.progressivoFinalePositivoMeseAux - monthRecap.getProgressivoFinaleNegativoMese();
    monthRecap.setProgressivoFinaleNegativoMeseImputatoProgressivoFinalePositivoMese(
        monthRecap.getProgressivoFinaleNegativoMese());
    return;

  }

  private void assegnaStraordinari(ContractMonthRecap monthRecap) {
    monthRecap.progressivoFinalePositivoMeseAux =
        monthRecap.progressivoFinalePositivoMeseAux - monthRecap.getStraordinariMinuti();
  }

  private void assegnaRiposiCompensativi(ContractMonthRecap monthRecap) {
    //quello che assegno al monte ore passato
    if (monthRecap.getRiposiCompensativiMinuti() < monthRecap.getRemainingMinutesLastYear()) {
      monthRecap.setRemainingMinutesLastYear(
          monthRecap.getRemainingMinutesLastYear() - monthRecap.getRiposiCompensativiMinuti());
      monthRecap.setRiposiCompensativiMinutiImputatoAnnoPassato(
          monthRecap.getRiposiCompensativiMinuti());
      return;
    } else {
      monthRecap.setRiposiCompensativiMinutiImputatoAnnoPassato(
          monthRecap.getRemainingMinutesLastYear());
      monthRecap.setRemainingMinutesLastYear(0);
      monthRecap.setRiposiCompensativiMinuti(
          monthRecap.getRiposiCompensativiMinuti()
              -
              monthRecap.getRiposiCompensativiMinutiImputatoAnnoPassato());
    }

    //quello che assegno al monte ore corrente
    if (monthRecap.getRiposiCompensativiMinuti() < monthRecap.getRemainingMinutesCurrentYear()) {
      monthRecap.setRemainingMinutesCurrentYear(
          monthRecap.getRemainingMinutesCurrentYear() - monthRecap.getRiposiCompensativiMinuti());
      monthRecap.setRiposiCompensativiMinutiImputatoAnnoCorrente(
          monthRecap.getRiposiCompensativiMinuti());
      return;
    } else {
      monthRecap.setRiposiCompensativiMinutiImputatoAnnoCorrente(
          monthRecap.getRemainingMinutesCurrentYear());
      monthRecap.setRemainingMinutesCurrentYear(0);
      monthRecap.setRiposiCompensativiMinuti(
          monthRecap.getRiposiCompensativiMinuti()
              -
              monthRecap.getRiposiCompensativiMinutiImputatoAnnoCorrente());
    }
    //quello che assegno al progressivo positivo del mese
    monthRecap.progressivoFinalePositivoMeseAux =
        monthRecap.progressivoFinalePositivoMeseAux
            -
            monthRecap.getRiposiCompensativiMinuti();
    monthRecap.setRiposiCompensativiMinutiImputatoProgressivoFinalePositivoMese(
        monthRecap.getRiposiCompensativiMinuti());

  }
  
  private void assegnaRiposiCompensativiChiusuraEnte(ContractMonthRecap monthRecap) {
    //quello che assegno al monte ore passato
    if (monthRecap.getRiposiCompensativiChiusuraEnteMinuti() 
        < monthRecap.getRemainingMinutesLastYear()) {
      monthRecap.setRemainingMinutesLastYear(
          monthRecap.getRemainingMinutesLastYear() 
          - monthRecap.getRiposiCompensativiChiusuraEnteMinuti());
      monthRecap.setRiposiCompensativiChiusuraEnteMinutiImputatoAnnoPassato(monthRecap
          .getRiposiCompensativiChiusuraEnteMinuti());
      return;
    } else {
      monthRecap.setRiposiCompensativiChiusuraEnteMinutiImputatoAnnoPassato(monthRecap
          .getRemainingMinutesLastYear());
      monthRecap.setRemainingMinutesLastYear(0);
      monthRecap.setRiposiCompensativiChiusuraEnteMinuti(monthRecap
          .getRiposiCompensativiChiusuraEnteMinuti()
          - monthRecap.getRiposiCompensativiChiusuraEnteMinutiImputatoAnnoPassato());
    }

    //quello che assegno al monte ore corrente
    if (monthRecap.getRiposiCompensativiChiusuraEnteMinuti() 
          < monthRecap.getRemainingMinutesCurrentYear()) {
      monthRecap.setRemainingMinutesCurrentYear(
          monthRecap.getRemainingMinutesCurrentYear() 
            - monthRecap.getRiposiCompensativiChiusuraEnteMinuti());
      monthRecap.setRiposiCompensativiChiusuraEnteMinutiImputatoAnnoCorrente(
          monthRecap.getRiposiCompensativiChiusuraEnteMinuti());
      return;
    } else {
      monthRecap.setRiposiCompensativiChiusuraEnteMinutiImputatoAnnoCorrente(
          monthRecap.getRemainingMinutesCurrentYear());
      monthRecap.setRemainingMinutesCurrentYear(0);
      monthRecap.setRiposiCompensativiChiusuraEnteMinuti(
          monthRecap.getRiposiCompensativiChiusuraEnteMinuti()
              -
              monthRecap.getRiposiCompensativiChiusuraEnteMinutiImputatoAnnoCorrente());
    }
    //quello che assegno al progressivo positivo del mese
    monthRecap.progressivoFinalePositivoMeseAux =
        monthRecap.progressivoFinalePositivoMeseAux
            -
            monthRecap.getRiposiCompensativiChiusuraEnteMinuti();
    monthRecap.setRiposiCompensativiChiusuraEnteMinutiImputatoProgressivoFinalePositivoMese(
        monthRecap.getRiposiCompensativiChiusuraEnteMinuti());
  }
}