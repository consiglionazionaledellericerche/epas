package manager.services.vacations;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.gdata.util.common.base.Preconditions;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import manager.services.vacations.VacationsTypeResult.TypeVacation;

import models.Absence;
import models.Contract;
import models.VacationCode;
import models.VacationPeriod;

import org.joda.time.LocalDate;

import java.util.List;

/**
 * Builder del riepilogo ferie e delle strutture dati annesse (i passaggi intermedi).
 * 
 * @author alessandro
 */
public class VacationsRecapBuilder {
  
  public static final int YEAR_VACATION_UPPER_BOUND = 28;

  /**
   * Costruisce il VacationRecap. <br>
   * La dateRecap è la data del riepilogo (tipicamente oggi) serve per: <br>
   * - stabilire se le ferie dell'anno passato sono scadute <br>
   * - calcolare la situazione delle maturate.
   *  
   * @param year anno
   * @param contract contratto
   * @param absencesToConsider assenze da considerare
   * @param dateRecap data del riepilogo.  
   * @param expireDateLastYear data scadenza ferie anno passato per l'anno precedente
   * @param expireDateCurrentYear data scadenza ferie anno passato per l'anno corrente
   * @return il recap.
   */
  public VacationsRecap buildVacationRecap(int year, Contract contract, 
      List<Absence> absencesToConsider, LocalDate dateRecap, LocalDate expireDateLastYear, 
      LocalDate expireDateCurrentYear) {

    VacationsRecap vacationsRecap = new VacationsRecap();
    
    DateInterval contractDateInterval =
        new DateInterval(contract.getBeginDate(), contract.calculatedEnd());
    
    VacationsRecapTempData tempData = VacationsRecapTempData.builder()
        .year(year)
        .absencesToConsider(absencesToConsider)
        .contract(contract).build();
    
    vacationsRecap.setVacationsRequest(VacationsRequest.builder()
        .year(year)
        .contract(contract)
        .contractDateInterval(contractDateInterval)
        .accruedDate(dateRecap)
        .contractVacationPeriod(contract.getVacationPeriods())
        .postPartumUsed(tempData.getPostPartum())
        .expireDateLastYear(expireDateLastYear)
        .expireDateCurrentYear(expireDateCurrentYear)
        .build());

    vacationsRecap.setVacationsLastYear(buildVacationsTypeResult(
        vacationsRecap.getVacationsRequest(),
        TypeVacation.VACATION_LAST_YEAR,
        expireDateLastYear,
        FluentIterable.from(tempData.getList32PreviouYear())
        .append(tempData.getList31RequestYear())
        .append(tempData.getList37RequestYear()).toList(),
        tempData.getSourceVacationLastYearUsed()));

    vacationsRecap.setVacationsCurrentYear(buildVacationsTypeResult(
        vacationsRecap.getVacationsRequest(),
        TypeVacation.VACATION_CURRENT_YEAR,
        expireDateCurrentYear,
        FluentIterable.from(tempData.getList32RequestYear())
        .append(tempData.getList31NextYear())
        .append(tempData.getList37NextYear()).toList(),
        tempData.getSourceVacationCurrentYearUsed()));

    vacationsRecap.setPermissions(buildVacationsTypeResult(
        vacationsRecap.getVacationsRequest(),
        TypeVacation.PERMISSION_CURRENT_YEAR,
        new LocalDate(year, 12, 31),
        FluentIterable.from(tempData.getList94RequestYear()).toList(),
        tempData.getSourcePermissionUsed()));

    return vacationsRecap;
  }
  
  
  /**
   * Costruisce il risultato della richiesta per il TypeVacation specifico.
   * @param vacationsRequest richista
   * @param typeVacation tipo assenza
   * @param absencesUsed lista delle assenze usate
   * @param sourced dati iniziali
   * @return il risultato per il tipo.
   */
  private VacationsTypeResult buildVacationsTypeResult(VacationsRequest vacationsRequest,
      TypeVacation typeVacation, LocalDate typeExpireDate, ImmutableList<Absence> absencesUsed, 
      int sourced) {
    
    if (vacationsRequest.getContract().person.surname.equals("Bordone")) {
      int i = 0;
    }
    
    
    VacationsTypeResult vacationsTypeResult = new VacationsTypeResult();
    vacationsTypeResult.setVacationsRequest(vacationsRequest);
    vacationsTypeResult.setAbsencesUsed(absencesUsed);
    vacationsTypeResult.setSourced(sourced);
    vacationsTypeResult.setTypeVacation(typeVacation);

    // Intervallo totale
    DateInterval totalInterval = new DateInterval(new LocalDate(vacationsRequest.getYear(), 1, 1),
        new LocalDate(vacationsRequest.getYear(), 12, 31));
    if (typeVacation.equals(TypeVacation.VACATION_LAST_YEAR)) {
      totalInterval = new DateInterval(new LocalDate(vacationsRequest.getYear() - 1, 1, 1),
          new LocalDate(vacationsRequest.getYear() - 1, 12, 31));
    }
    
    //Expired
    if (vacationsRequest.getAccruedDate().isAfter(typeExpireDate)) {
      vacationsTypeResult.setExpired(true);
    }
    
    //Lower/Upper bound
    vacationsTypeResult.setLowerLimit(totalInterval.getBegin());
    if (vacationsRequest.getContractDateInterval().getBegin().isAfter(totalInterval.getBegin())) {
      vacationsTypeResult.setLowerLimit(vacationsRequest.getContractDateInterval().getBegin());
      vacationsTypeResult.setContractLowerLimit(true);
    }
    vacationsTypeResult.setUpperLimit(typeExpireDate);
    if (vacationsRequest.getContractDateInterval().getEnd().isBefore(typeExpireDate)) {
      vacationsTypeResult.setUpperLimit(vacationsRequest.getContractDateInterval().getEnd());
      vacationsTypeResult.setContractUpperLimit(true);
    }

    // Intervallo accrued
    LocalDate beginAccrued = totalInterval.getBegin();
    LocalDate endAccrued = totalInterval.getEnd();
    if (endAccrued.isAfter(vacationsRequest.getAccruedDate())) {
      endAccrued = vacationsRequest.getAccruedDate();
    }
    Preconditions.checkState(!beginAccrued.isAfter(endAccrued));
    DateInterval accruedInterval = new DateInterval(beginAccrued, endAccrued);
    
    //Intersezioni col contratto.
    accruedInterval = DateUtility.intervalIntersection(accruedInterval,
        vacationsRequest.getContractDateInterval());
    totalInterval = DateUtility.intervalIntersection(totalInterval,
        vacationsRequest.getContractDateInterval());

    // Costruisco il riepilogo delle totali.
    vacationsTypeResult.setTotalResult(buildAccruedResult(vacationsTypeResult, totalInterval));
    
    // Fix dei casi particolari nel caso del riepilogo totali quando capita il cambio piano.
    adjustDecision(vacationsTypeResult.getTotalResult());

    // Costruisco il riepilogo delle maturate.
    vacationsTypeResult.setAccruedResult(buildAccruedResult(vacationsTypeResult, accruedInterval));

    return vacationsTypeResult;
  }
  
  /**
   * Risultato per la richiesta in vacationsTypeResult rispetto all'interval passato. <br>
   * Esistono tipicamente due accruedResult: quello delle assenze totali e quello delle assenze 
   * maturate. 
   * @param vacationsTypeResult risultato della richiesta.
   * @param interval intervallo.
   * @return il risultato.
   */
  private AccruedResult buildAccruedResult(VacationsTypeResult vacationsTypeResult, 
      DateInterval interval) {
    
    AccruedResult accruedResult = new AccruedResult();
    accruedResult.setVacationsResult(vacationsTypeResult);
    accruedResult.setInterval(interval);
    accruedResult.setAccruedConverter(AccruedConverter.builder().build());
    
    for (VacationPeriod vp : vacationsTypeResult.getVacationsRequest()
        .getContractVacationPeriod()) {

      AccruedResultInPeriod accruedResultInPeriod = buildAccruedResultInPeriod(
          accruedResult,
          DateUtility.intervalIntersection(interval, vp.getDateInterval()),
          vp.vacationCode,
          vacationsTypeResult.getVacationsRequest().getPostPartumUsed());
      
      addResult(accruedResult, accruedResultInPeriod); 
    }
    
    return accruedResult;
  }
  
  
  /**
   * Costruisce il sotto risultato relativo al vacationCode per l'intervallo interval.
   * @param parentAccruedResult l'accruedResult
   * @param interval l'intervallo del vacationPeriod
   * @param vacationCode il codice
   * @param absences le assenze da considerare.
   * @return il risultato nel periodo.
   */
  private AccruedResultInPeriod buildAccruedResultInPeriod(AccruedResult parentAccruedResult,
      DateInterval interval, VacationCode vacationCode, List<Absence> absences) {
    
    AccruedConverter accruedConverter = new AccruedConverter();
    
    AccruedResultInPeriod accruedResultInPeriod = new AccruedResultInPeriod();
    accruedResultInPeriod.setVacationsResult(parentAccruedResult.getVacationsResult());
    accruedResultInPeriod.setInterval(interval);
    accruedResultInPeriod.setAccruedConverter(AccruedConverter.builder().build());
    accruedResultInPeriod.setVacationCode(vacationCode);

    if (accruedResultInPeriod.getInterval() == null) {
      return accruedResultInPeriod;
    }
    
    //set post partum absences
    for (Absence ab : absences) {
      if (DateUtility.isDateIntoInterval(ab.personDay.date, accruedResultInPeriod.getInterval())) {
        accruedResultInPeriod.getPostPartum().add(ab);
      }
    }
    
    //computation
    
    //TODO: verificare che nel caso dei permessi non considero i giorni postPartum.
    accruedResultInPeriod.setDays(DateUtility.daysInInterval(accruedResultInPeriod.getInterval()) 
        - accruedResultInPeriod.getPostPartum().size());

    int accrued = 0;
    
    //calcolo i giorni maturati col metodo di conversione
    if (accruedResultInPeriod.getVacationsResult().getTypeVacation()
        .equals(TypeVacation.PERMISSION_CURRENT_YEAR)) {

      //this.days = DateUtility.daysInInterval(this.interval);

      if (accruedResultInPeriod.getVacationCode().description.equals("21+3")
          || accruedResultInPeriod.getVacationCode().description.equals("22+3")) {

        accrued = accruedConverter.permissionsPartTime(accruedResultInPeriod.getDays());
      } else {
        accrued = accruedConverter.permissions(accruedResultInPeriod.getDays());
      }

    } else {

      //this.days = DateUtility.daysInInterval(this.interval) - this.postPartum.size();

      if (accruedResultInPeriod.getVacationCode().description.equals("26+4")) {
        accrued = accruedConverter.vacationsLessThreeYears(accruedResultInPeriod.getDays());
      }
      if (accruedResultInPeriod.getVacationCode().description.equals("28+4")) {
        accrued = accruedConverter.vacationsMoreThreeYears(accruedResultInPeriod.getDays());
      }
      if (accruedResultInPeriod.getVacationCode().description.equals("21+3")) {
        accrued = accruedConverter.vacationsPartTimeLessThreeYears(accruedResultInPeriod.getDays());
      }
      if (accruedResultInPeriod.getVacationCode().description.equals("22+3")) {
        accrued = accruedConverter.vacationsPartTimeMoreThreeYears(accruedResultInPeriod.getDays());
      }
    }
    
    accruedResultInPeriod.setAccrued(accrued);
    
    return accruedResultInPeriod;
  }
  
  /**
   * Somma il risultato in period relativo ad un vacationPeriod ad accruedResult.
   * @param accruedResult accruedResult
   * @param accruedResultInPeriod il sotto-risultato da sommare all'accruedResult
   * @return l'accruedResult.
   */
  private AccruedResult addResult(AccruedResult accruedResult, 
      AccruedResultInPeriod accruedResultInPeriod) {

    accruedResult.getAccruedResultsInPeriod().add(accruedResultInPeriod);
    accruedResult.getPostPartum().addAll(accruedResultInPeriod.getPostPartum());
    accruedResult.setDays(accruedResult.getDays() + accruedResultInPeriod.getDays());
    accruedResult.setAccrued(accruedResult.getAccrued() + accruedResultInPeriod.getAccrued());
    return accruedResult;
  }

  /**
   * Aggiusta il calcolo di ferie e permessi totali. <br>
   * Si applica solo all'accruedResult delle totali.
   * 
   * @param accruedResult il risultato da aggiustare.
   * @return il risultato aggiustato.
  */
  private AccruedResult adjustDecision(AccruedResult accruedResult) {

    if (accruedResult.getInterval() == null) {
      return accruedResult;  //niente da aggiustare..
    }

    // per ora i permessi non li aggiusto.
    if (accruedResult.getVacationsResult().getTypeVacation()
        .equals(TypeVacation.PERMISSION_CURRENT_YEAR)) {
      return accruedResult;
    }

    if (accruedResult.getAccruedResultsInPeriod().isEmpty()) {
      return accruedResult;
    }

    DateInterval yearInterval = DateUtility
        .getYearInterval(accruedResult.getVacationsResult().getVacationsRequest().getYear());

    int totalYearPostPartum = 0;
    int totalVacationAccrued = 0;

    AccruedResultInPeriod minAccruedResultInPeriod = null;


    for (AccruedResultInPeriod accruedResultInPeriod : accruedResult.getAccruedResultsInPeriod()) {

      if (minAccruedResultInPeriod == null) {

        minAccruedResultInPeriod = accruedResultInPeriod;

      } else if (accruedResultInPeriod.getVacationCode().vacationDays
          < minAccruedResultInPeriod.getVacationCode().vacationDays ) {

        minAccruedResultInPeriod = accruedResultInPeriod;
      }
      totalYearPostPartum += accruedResultInPeriod.getPostPartum().size();
      totalVacationAccrued += accruedResultInPeriod.getAccrued();

    }

    //Aggiusto perchè l'algoritmo ne ha date troppe.
    if (totalVacationAccrued > YEAR_VACATION_UPPER_BOUND) {
      accruedResult.setFixed(YEAR_VACATION_UPPER_BOUND - totalVacationAccrued);  //negative
    }

    //Aggiusto perchè l'algoritmo ne ha date troppo poche.
    //Condizione: no assenze post partum e periodo che copre tutto l'anno.
    if (totalYearPostPartum == 0
        && accruedResult.getInterval().getBegin().equals(yearInterval.getBegin())
            && accruedResult.getInterval().getEnd().equals(yearInterval.getEnd())) {

      if (minAccruedResultInPeriod.getVacationCode().vacationDays
          > totalVacationAccrued) {

        accruedResult.setFixed(minAccruedResultInPeriod.getVacationCode().vacationDays
            - totalVacationAccrued); //positive
      }
    }

    return accruedResult;
  }
}
