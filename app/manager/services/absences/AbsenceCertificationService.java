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

package manager.services.absences;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import dao.absences.AbsenceComponentDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import manager.attestati.dto.internal.CruscottoDipendente;
import manager.attestati.dto.internal.CruscottoDipendente.SituazioneDipendenteAssenze;
import manager.attestati.dto.internal.CruscottoDipendente.SituazioneParametriControllo;
import manager.attestati.dto.show.CodiceAssenza;
import manager.attestati.service.CertificationService;
import manager.services.absences.certifications.CertificationYearSituation;
import manager.services.absences.certifications.CertificationYearSituation.AbsenceSituation;
import manager.services.absences.certifications.CertificationYearSituation.AbsenceSituationType;
import manager.services.absences.certifications.CodeComparation;
import manager.services.absences.model.PeriodChain;
import manager.services.absences.model.VacationSituation;
import models.Person;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.GroupAbsenceType;
import models.absences.InitializationGroup;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.definitions.DefaultAbsenceType;
import models.absences.definitions.DefaultGroup;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import play.cache.Cache;

/**
 * Servizi di comparazione fra assenze epas e assenze attestati.
 *
 * @author Alessandro Martelli
 *
 */
@Slf4j
public class AbsenceCertificationService {

  private final AbsenceComponentDao absenceComponentDao;
  private final CertificationService certificationService;
  private final AbsenceService absenceService;
  private final IWrapperFactory wrapperFactory;

  /**
   * Injection.
   *
   * @param absenceComponentDao injected
   * @param certificationService injected
   * @param absenceService injected
   */
  @Inject
  public AbsenceCertificationService(AbsenceComponentDao absenceComponentDao,
      CertificationService certificationService, AbsenceService absenceService,
      IWrapperFactory wrapperFactory) {
    this.absenceComponentDao = absenceComponentDao;
    this.certificationService = certificationService;
    this.absenceService = absenceService;
    this.wrapperFactory = wrapperFactory;
  }
  
  /**
   * Una chiave per il certification year situation.
   */
  private String cysKey(Person person, int year) {
    return "cys-" + "-" + person.id + "-" + year;
  }
  
  /**
   * Una chiave per il cruscotto.
   */
  private String crKey(Person person, int year) {
    return "cruscotto-" + "-" + person.id + "-" + year;
  }

  /**
   * La situatione della persona dalla cache se caricata.
   *
   * @param person person 
   * @param year anno
   */
  public Optional<CertificationYearSituation> certificationYearSituationCached(Person person, 
      int year) {
    return Optional.fromNullable(
        (CertificationYearSituation) Cache.get(cysKey(person, year)));

  }


  /**
   * Situazione assenze epas/attestati.
   *
   * @param person persona
   * @param year anno
   * @param cache prova a prelevare il certification year situation dalla cache
   * @return situazione
   */
  public CertificationYearSituation buildCertificationYearSituation(Person person, int year, 
      boolean cache) {
    
    if (cache) {
      CertificationYearSituation situationCached = (CertificationYearSituation)
          Cache.get(cysKey(person, year));
      if (situationCached != null) {
        return situationCached;
      }
    }
    
    CruscottoDipendente cruscottoCurrent = (CruscottoDipendente) Cache.get(crKey(person, year));
    if (cruscottoCurrent == null) {
      try {
        log.debug("Il cruscotto di {} anno {} non era cachato.", person.fullName(), year);        
        cruscottoCurrent = certificationService.getCruscottoDipendente(person, year);
        Cache.add(crKey(person, year), cruscottoCurrent);
      } catch (Exception ex) {
        log.info("Impossibile prelevare il cruscotto di {} anno {}", person.fullName(), year);
        return null;
      }
    }
    
    if (cruscottoCurrent == null) {
      return null;
    }
    
    CruscottoDipendente cruscottoPrev = (CruscottoDipendente) Cache.get(crKey(person, year - 1));
    if (cruscottoPrev == null) {
      try {
        log.debug("Il cruscotto di {} anno {} non era cachato.", person.fullName(), year - 1);
        cruscottoPrev = certificationService.getCruscottoDipendente(person, year - 1);
        Cache.add(crKey(person, year - 1), cruscottoPrev);
      } catch (Exception ex) {
        log.info("Impossibile prelevare il cruscotto "
            + "anno precedente di {} per l'anno {}", person.fullName(), year);
        cruscottoPrev = null;
      }
    }
    
    CertificationYearSituation situation = new CertificationYearSituation();
    
    situation.year = cruscottoCurrent.annoSituazione;
    
    situation.beginDate = new LocalDate(cruscottoCurrent.dipendente.dataAssunzione);
    if (cruscottoCurrent.dipendente.dataCessazione != null) {
      situation.endDate = new LocalDate(cruscottoCurrent.dipendente.dataCessazione);
    }
    
    //MAPPONA CON TUTTI I CODICI IN ATTESTATI
    Map<String, Set<LocalDate>> mappona = Maps.newHashMap();
    
    for (SituazioneDipendenteAssenze sda : cruscottoCurrent.situazioneDipendenteAssenze) {
      putDates(mappona, sda.codice.codice, sda.codeDates());
    }
    for (SituazioneParametriControllo spc : cruscottoCurrent.situazioneParametriControllo) {
      Map<String, Set<LocalDate>> codesDates = spc.codesDates();
      for (String code : codesDates.keySet()) {
        putDates(mappona, code, codesDates.get(code));
      }
    }
      
    if (cruscottoPrev != null) {
      for (SituazioneDipendenteAssenze sda : cruscottoPrev.situazioneDipendenteAssenze) {
        putDates(mappona, sda.codice.codice, sda.codeDates());
      }
      for (SituazioneParametriControllo spc : cruscottoPrev.situazioneParametriControllo) {
        Map<String, Set<LocalDate>> codesDates = spc.codesDates();
        for (String code : codesDates.keySet()) {
          putDates(mappona, code, codesDates.get(code));
        }
      }
    }
    
    Map<String, Set<LocalDate>> inEpas = Maps.newHashMap();
    Map<String, Set<LocalDate>> notInEpas = Maps.newHashMap();
    for (String code : mappona.keySet()) {
      for (LocalDate date : mappona.get(code)) {
        if (!absenceIsInEpas(person, code, date)) {
          putDates(notInEpas, code, Sets.newHashSet(date));
        } else {
          putDates(inEpas, code, Sets.newHashSet(date));
        }
      }
    }
    
    situation.certificationMap = mappona;
      
    //Inizio a costruire le situazioni
    
    //1) ferie e permessi legge
    IWrapperPerson wrPerson = wrapperFactory.create(person);
    buildVacationSituation(situation, wrPerson, year, inEpas, notInEpas);
      
    //2) malattia 3 anni
    LocalDate from = LocalDate.now().withDayOfMonth(1).minusYears(3).plusMonths(1);
    LocalDate to = LocalDate.now().dayOfMonth().withMaximumValue();
    buildGenericSituation(situation, person, AbsenceSituationType.MALATTIA_3_ANNI, 
        DefaultGroup.MALATTIA_3_ANNI, LocalDate.now(), Optional.of(from), Optional.of(to), 
        inEpas, notInEpas);
    
    //3) astensione facoltativa
    buildGenericSituation(situation, person, AbsenceSituationType.ASTENSIONE_FIGLIO_1, 
        DefaultGroup.G_23, LocalDate.now(), Optional.absent(), Optional.absent(), 
        inEpas, notInEpas);
    buildGenericSituation(situation, person, AbsenceSituationType.ASTENSIONE_FIGLIO_2, 
        DefaultGroup.G_232, LocalDate.now(), Optional.absent(), Optional.absent(), 
        inEpas, notInEpas);
    buildGenericSituation(situation, person, AbsenceSituationType.ASTENSIONE_FIGLIO_3, 
        DefaultGroup.G_233, LocalDate.now(), Optional.absent(), Optional.absent(), 
        inEpas, notInEpas);
    
    //4) riduce ferie
    from = new LocalDate(LocalDate.now().getYear(), 1, 1);
    to = new LocalDate(LocalDate.now().getYear(), 12, 31);
    buildGenericSituation(situation, person, AbsenceSituationType.RIDUCE_FERIE_ANNO_CORRENTE, 
        DefaultGroup.RIDUCE_FERIE_CNR, to, Optional.of(from), Optional.of(to), 
        inEpas, notInEpas);
    from = from.minusYears(1);
    to = to.minusYears(1);    
    buildGenericSituation(situation, person, AbsenceSituationType.RIDUCE_FERIE_ANNO_PRECEDENTE, 
        DefaultGroup.RIDUCE_FERIE_CNR, to, Optional.of(from), Optional.of(to),
        inEpas, notInEpas);
    
    //5) riposo compensativo
    AbsenceSituation compensatorySituation = buildGenericSituation(situation, person, 
        AbsenceSituationType.RIPOSO_COMPENSATIVO, 
        DefaultGroup.RIPOSI_CNR_ATTESTATI, LocalDate.now(), Optional.absent(), Optional.absent(),
        inEpas, notInEpas);
    patchCompensatoryRest(compensatorySituation, wrPerson);
    
    //6) permesso personale
    from = new LocalDate(LocalDate.now().getYear(), 1, 1);
    to = new LocalDate(LocalDate.now().getYear(), 12, 31);
    buildGenericSituation(situation, person, AbsenceSituationType.PERMESSO_PERSONALI, 
        DefaultGroup.G_661, to, Optional.of(from), Optional.of(to), 
        inEpas, notInEpas);
    
    //7) tutte le altre assenze dell'anno corrente non rifinite in altra situation
    buildOtherAbsences(situation, mappona, inEpas, notInEpas);
    
    //spostamenti particolari fra liste
    patchPostPartumH7(person, situation);
    patchStudy(situation);
    patchSentCertifications(situation.absenceSituations, wrPerson);
    
    
    //metto in cache
    Cache.safeDelete(cysKey(person, year));
    Cache.add(cysKey(person, year), situation);
    return situation;
  }
  
  /**
   * Le assenze da processare non presenti nella map con quei codes. 
   */
  private List<Absence> absenceNotInAttestati(List<Absence> absencesToProcess, 
      Map<String, Set<LocalDate>> map, List<String> codes, 
      Optional<LocalDate> from, Optional<LocalDate> to) { 
    Set<LocalDate> dates = Sets.newHashSet();
    for (String code : codes) {
      if (map.get(code) != null) {
        dates.addAll(map.get(code));
      }
    }
    List<Absence> list = Lists.newArrayList();
    for (Absence absence : absencesToProcess) {
      if (from.isPresent() && absence.getAbsenceDate().isBefore(from.get())) {
        continue;
      }
      if (to.isPresent() && absence.getAbsenceDate().isAfter(to.get())) {
        continue;
      }
      if (!dates.contains(absence.getAbsenceDate())) {
        list.add(absence);
      }
    }
    return list;
  }
  
  private AbsenceSituation buildGenericSituation(CertificationYearSituation situation, 
      Person person, AbsenceSituationType situationType, 
      DefaultGroup group, LocalDate recap, Optional<LocalDate> from, Optional<LocalDate> to,
      Map<String, Set<LocalDate>> inEpas, Map<String, Set<LocalDate>> notInEpas) {
    
    AbsenceSituation absenceSituation = new AbsenceSituation(situationType);
    List<String> allCodes = Lists.newArrayList();
    Set<DefaultAbsenceType> allType = Sets.newHashSet();
    DefaultGroup currentGroup = group;
    while (currentGroup != null) {
      allType.addAll(currentGroup.takable.takableCodes);
      if (currentGroup.complation != null) {
        allType.addAll(currentGroup.complation.replacingCodes);
      }
      currentGroup = currentGroup.nextGroupToCheck;
    }
    for (DefaultAbsenceType type : allType) {
      putDatesInterval(absenceSituation.datesPerCodeOk, 
          type.certificationCode, inEpas.get(type.certificationCode), from, to); 
      putDatesInterval(absenceSituation.toAddAutomatically, 
          type.certificationCode, notInEpas.get(type.certificationCode), from, to); 
      allCodes.add(type.getCode());
    }
    GroupAbsenceType groupAbsenceType = absenceComponentDao
        .groupAbsenceTypeByName(group.name()).get();
    PeriodChain periodChain = absenceService.residual(person, groupAbsenceType, recap);
    absenceSituation.notPresent = absenceNotInAttestati(periodChain.relevantAbsences(true), 
        absenceSituation.datesPerCodeOk, allCodes, from, to);
    
    //tag child is missing (da migliorare)
    if (!absenceSituation.datesPerCodeOk.isEmpty() 
        || !absenceSituation.toAddAutomatically.isEmpty()) {
      if (group.equals(DefaultGroup.G_23) && periodChain.childIsMissing()) {
        absenceSituation.firstChildMissing = true;
      }
      if (group.equals(DefaultGroup.G_232) && periodChain.childIsMissing()) {
        absenceSituation.secondChildMissing = true;
      }
      if (group.equals(DefaultGroup.G_233) && periodChain.childIsMissing()) {
        absenceSituation.thirdChildMissing = true;
      }
    }
    
    situation.absenceSituations.add(absenceSituation);
    return absenceSituation;
  }

  private void putDates(Map<String, Set<LocalDate>> map, String code, Set<LocalDate> dates) {
    if (dates == null || dates.isEmpty()) {
      return;
    }
    Set<LocalDate> set = map.get(code);
    if (set == null) {
      set = Sets.newHashSet();
      map.put(code, set);
    }
    set.addAll(dates);
  }
  
  private void removeDates(Map<String, Set<LocalDate>> map, String code, Set<LocalDate> dates) {
    if (dates == null || dates.isEmpty()) {
      return;
    }
    Set<LocalDate> set = map.get(code);
    if (set == null) {
      set = Sets.newHashSet();
      map.put(code, set);
    }
    set.removeAll(dates);
  }
  
  private void putDatesInterval(Map<String, Set<LocalDate>> map, String code, Set<LocalDate> dates, 
      Optional<LocalDate> from, Optional<LocalDate> to) {
    if (dates == null || dates.isEmpty()) {
      return;
    }
    Set<LocalDate> set = map.get(code);
    if (set == null) {
      set = Sets.newHashSet();
      map.put(code, set);
    }
    for (LocalDate date : dates) {
      if (from.isPresent() && date.isBefore(from.get())) {
        continue;
      }
      if (to.isPresent() && date.isAfter(to.get())) {
        continue;
      }
      set.add(date);
    }
  }
  
  private Set<LocalDate> datesNotPresent(Map<String, Set<LocalDate>> map, 
      String code, Set<LocalDate> dates, Integer year) {
    Set<LocalDate> set = Sets.newHashSet();
    for (LocalDate date : dates) {
      if (map.get(code) == null || !map.get(code).contains(date)) {
        if (date.getYear() == year) {
          set.add(date);
        }
      }
    }
    return set;
  }
  
  private boolean absenceIsInEpas(Person person, String code, LocalDate date) {
    
    Set<AbsenceType> certificationCodes = Sets.newHashSet();
    
    Optional<AbsenceType> absenceType = absenceComponentDao.absenceTypeByCode(code);
    if (!absenceType.isPresent() 
        || (absenceType.get().getCertificateCode() != null 
        && !absenceType.get().getCertificateCode().equalsIgnoreCase(code))) {
      
      //Se non lo trovo oppure il codice in attestati è diverso da quello trovato
      //Lo cerco per codice attestati. Si potrebbe migliorare popolando per ogni codice 
      //la colonna codice attestati.
    } else {
      certificationCodes.add(absenceType.get());
    }
    
    certificationCodes.addAll(absenceComponentDao.absenceTypesByCertificationCode(code));
    
    if (certificationCodes.isEmpty()) {
      return false;
    }

    List<Absence> absences = Lists.newArrayList();
    for (AbsenceType certificationType : certificationCodes) {
      absences.addAll(absenceComponentDao.orderedAbsences(person, date, date, 
          Sets.newHashSet(certificationType)));
    }

    if (absences.isEmpty()) {
      return false;
    }
    
    return true;
  }
  
  private CertificationYearSituation buildVacationSituation(CertificationYearSituation situation, 
      IWrapperPerson wrPerson, int year, 
      Map<String, Set<LocalDate>> mapInEpas, Map<String, Set<LocalDate>> mapNotInEpas) {

    //Situazione in epas
    GroupAbsenceType vacationGroup = absenceComponentDao
        .groupAbsenceTypeByName(DefaultGroup.FERIE_CNR.name()).get();
    VacationSituation vacationSituation = absenceService.buildVacationSituation(
        wrPerson.getCurrentContract().get(), year, vacationGroup, Optional.absent(), false);
    
    //Situazione ferie anno selezionato
    AbsenceSituation vacationYear = new AbsenceSituation(AbsenceSituationType.FERIE_ANNO_CORRENTE);
    
    final String code32 = DefaultAbsenceType.A_32.getCode();
    final String code31 = DefaultAbsenceType.A_31.getCode();
    final String code37 = DefaultAbsenceType.A_37.getCode();
    final String code94 = DefaultAbsenceType.A_94.getCode();
    
    putDatesVacation(vacationYear.datesPerCodeOk, mapInEpas, code32, year);
    putDatesVacation(vacationYear.toAddAutomatically, mapNotInEpas, code32, year);
    vacationYear.notPresent = absenceNotInAttestati(
        vacationSituation.currentYear.absencesUsed(), 
        vacationYear.datesPerCodeOk, Lists.newArrayList(code32),
        Optional.absent(), Optional.absent());
    
    //Situazione ferie anno precedente
    AbsenceSituation vacationPreviousYear = new AbsenceSituation(
        AbsenceSituationType.FERIE_ANNO_PRECEDENTE);
    
    putDatesVacation(vacationPreviousYear.datesPerCodeOk, mapInEpas, code32, year - 1);
    putDatesVacation(vacationPreviousYear.datesPerCodeOk, mapInEpas, code31, year);
    putDatesVacation(vacationPreviousYear.datesPerCodeOk, mapInEpas, code37, year);
    putDatesVacation(vacationPreviousYear.toAddAutomatically, mapNotInEpas, code32,  year - 1);
    putDatesVacation(vacationPreviousYear.toAddAutomatically, mapNotInEpas, code31,  year);
    putDatesVacation(vacationPreviousYear.toAddAutomatically, mapNotInEpas, code37,  year);
    if (vacationSituation.lastYear != null) {
      vacationPreviousYear.notPresent = absenceNotInAttestati(
          vacationSituation.lastYear.absencesUsed(),
          vacationPreviousYear.datesPerCodeOk, Lists.newArrayList(code32, code31, code37),
          Optional.absent(), Optional.absent());
    }
    
    //Situazione permessi legge anno selezionato
    AbsenceSituation permissions = new AbsenceSituation(AbsenceSituationType.PERMESSI_LEGGE);
    
    putDatesVacation(permissions.datesPerCodeOk, mapInEpas, code94,  year);
    putDatesVacation(permissions.toAddAutomatically, mapNotInEpas, code94,  year);
    permissions.notPresent = absenceNotInAttestati(
        vacationSituation.permissions.absencesUsed(), 
        permissions.datesPerCodeOk, Lists.newArrayList(code94),
        Optional.absent(), Optional.absent());
    
    situation.absenceSituations.add(vacationYear);
    situation.absenceSituations.add(vacationPreviousYear);
    situation.absenceSituations.add(permissions);

    return situation;
    
  }
  
  private CertificationYearSituation buildOtherAbsences(CertificationYearSituation situation, 
      Map<String, Set<LocalDate>> mappona,
      Map<String, Set<LocalDate>> inEpas, Map<String, Set<LocalDate>> notInEpas) {
    
    Map<String, Set<LocalDate>> dispatched = Maps.newHashMap();
    for (AbsenceSituation absenceSituation : situation.absenceSituations) {
      for (String code : mappona.keySet()) {
        putDates(dispatched, code, absenceSituation.datesPerCodeOk.get(code));
        putDates(dispatched, code, absenceSituation.toAddAutomatically.get(code));
        putDates(dispatched, code, absenceSituation.toAddManually.get(code));
      }
    }
    Map<String, Set<LocalDate>> datesPerCodeOk = Maps.newHashMap();
    Map<String, Set<LocalDate>> toAddAutomatically = Maps.newHashMap();
    for (String code : inEpas.keySet()) {
      datesPerCodeOk.put(code, datesNotPresent(dispatched, code, inEpas.get(code), situation.year));
    }
    for (String code : notInEpas.keySet()) {
      toAddAutomatically
      .put(code, datesNotPresent(dispatched, code, notInEpas.get(code), situation.year));
    }
    
    AbsenceSituation absenceSituation = new AbsenceSituation(AbsenceSituationType.ALTRI);
    absenceSituation.datesPerCodeOk = datesPerCodeOk;
    absenceSituation.toAddAutomatically = toAddAutomatically;
    
    //i remove sono troppo difficili e non servono.
    
    situation.absenceSituations.add(absenceSituation);
    
    return situation;
  }
  

  
  /**
   * Aggiunge alla map la data per quel codice. Se year è valorizzato prima di inserirla verifica 
   * che la data appartenga all'anno. 
   */
  private void putDatesVacation(Map<String, Set<LocalDate>> map, Map<String, Set<LocalDate>> source,
      String code, Integer year) {
    if (source.get(code) == null) {
      return;
    }
    Set<LocalDate> mapDates = map.get(code);
    if (mapDates == null) {
      mapDates = Sets.newHashSet();
      map.put(code, mapDates);
    }
    for (LocalDate date : source.get(code)) {
      if (year != null && date.getYear() != year) {
        continue;
      }
      map.get(code).add(date);
    }
  }
  
  private void patchCompensatoryRest(AbsenceSituation absenceSituation, IWrapperPerson wrPerson) {
    //??: se su attestati vi è distinzione fra riposo compensativo effettivo (91) e recuperi per
    // lavoro festivo (91F), questa patch che sposta le assenza importabili automaticamente
    // in importabili manualmente non serve più.
    Set<LocalDate> dates = absenceSituation
        .toAddAutomatically.get(DefaultAbsenceType.A_91.getCode()); 
    if (dates == null || dates.isEmpty()) {
      return;
    }
    LocalDate sourceDate = wrPerson.getCurrentContract().get().getSourceDateResidual();
    LocalDate beginContract = wrPerson.getCurrentContract().get().getBeginDate();
    Set<LocalDate> manually = Sets.newHashSet();
    for (LocalDate date : dates) {
      if (date.isBefore(beginContract)) {
        continue;
      }
      if (sourceDate == null || date.isAfter(sourceDate)) {
        manually.add(date);
      }
    }
    for (LocalDate date : manually) {
      dates.remove(date);
    }
    absenceSituation.toAddManually.put(DefaultAbsenceType.A_91.getCode(), manually);
  }
  
  
  /**
   * Se in una data ho un 89 da inserire automaticamente.
   *   - tolgo 89 da inserire automaticamente
   *   - inserisco 89 da inserire manualmente
   * Se quell'89 è precedente l'inizializzazione
   *   - tolgo 89 da inserire manualmente
   */
  private void patchStudy(CertificationYearSituation situation) {
    for (AbsenceSituation absenceSituation : situation.absenceSituations) {
      Set<LocalDate> dates = absenceSituation.toAddAutomatically.get(
          DefaultAbsenceType.A_89.getCode());
      if (dates == null) {
        continue;
      }
      removeDates(absenceSituation.toAddAutomatically, DefaultAbsenceType.A_89.getCode(), dates);
      removeDates(absenceSituation.toAddManually, DefaultAbsenceType.A_89.getCode(), dates);
    }
  }
  
  private void patchPostPartumInit(Person person, Map<String, Set<LocalDate>> mappona) {
    //Complete primo figlio
    patchPostPartumInitComplete(person, mappona, DefaultGroup.G_23,
        Sets.newHashSet(DefaultAbsenceType.A_25.getCode(), DefaultAbsenceType.A_25H7.getCode(), 
            DefaultAbsenceType.A_24.getCode(), DefaultAbsenceType.A_24H7.getCode()));
    patchPostPartumInitComplete(person, mappona, DefaultGroup.G_25,
        Sets.newHashSet(DefaultAbsenceType.A_24.getCode(), DefaultAbsenceType.A_24H7.getCode()));
    //Complete secondo figlio
    patchPostPartumInitComplete(person, mappona, DefaultGroup.G_232,
        Sets.newHashSet(DefaultAbsenceType.A_252.getCode(), DefaultAbsenceType.A_252H7.getCode(), 
            DefaultAbsenceType.A_242.getCode(), DefaultAbsenceType.A_242H7.getCode()));
    patchPostPartumInitComplete(person, mappona, DefaultGroup.G_252,
        Sets.newHashSet(DefaultAbsenceType.A_242.getCode(), DefaultAbsenceType.A_242H7.getCode()));
    //Complete terzo figlio
    patchPostPartumInitComplete(person, mappona, DefaultGroup.G_233,
        Sets.newHashSet(DefaultAbsenceType.A_253.getCode(), DefaultAbsenceType.A_253H7.getCode(), 
            DefaultAbsenceType.A_243.getCode(), DefaultAbsenceType.A_243H7.getCode()));
    patchPostPartumInitComplete(person, mappona, DefaultGroup.G_253,
        Sets.newHashSet(DefaultAbsenceType.A_243.getCode(), DefaultAbsenceType.A_243H7.getCode()));
    
  }
  
  private void patchPostPartumInitComplete(Person person, Map<String, Set<LocalDate>> mappona,  
      DefaultGroup group, Set<String> codes) {
    GroupAbsenceType groupAbsenceType = absenceComponentDao
        .groupAbsenceTypeByName(group.name()).get();
    for (InitializationGroup initialization : person.getInitializationGroups()) {
      if (initialization.getGroupAbsenceType().equals(groupAbsenceType)) {
        return; //inizializzazione già presente o importata... si cambia solo manualmente.
      }
    }
    List<LocalDate> list = Lists.newArrayList();
    for (String code : codes) {
      if (mappona.get(code) != null) {
        list.addAll(mappona.get(code));
      }
    }
    if (list.isEmpty()) {
      return;
    }
    Collections.sort(list);
    InitializationGroup initializationGroup = new InitializationGroup(person, groupAbsenceType, 
        list.iterator().next().minusDays(1));
    initializationGroup.setUnitsInput(group.takable.fixedLimit);
    initializationGroup.setAverageWeekTime(432);
    initializationGroup.save();
  }
  
  /**
   * Se in una data ho 23H7 da inserire automaticamente e nella stessa data in epas ho un 23.
   *  - tolgo il 23H7 da inserire automaticamente
   *  - tolgo il 23 da codici da rimuovere
   *  - inserisco il 23H7 ok
   *
   * @param situation absenceSituation
   */
  private void patchPostPartumH7(Person person, CertificationYearSituation situation) {
    for (AbsenceSituation abSit : situation.absenceSituations) {
      addAutomaticallyRelax(person, abSit, DefaultAbsenceType.A_23H7, DefaultAbsenceType.A_23);
      addAutomaticallyRelax(person, abSit, DefaultAbsenceType.A_25H7, DefaultAbsenceType.A_25);
      addAutomaticallyRelax(person, abSit, DefaultAbsenceType.A_24H7, DefaultAbsenceType.A_24);
      addAutomaticallyRelax(person, abSit, DefaultAbsenceType.A_232H7, DefaultAbsenceType.A_232);
      addAutomaticallyRelax(person, abSit, DefaultAbsenceType.A_252H7, DefaultAbsenceType.A_252);
      addAutomaticallyRelax(person, abSit, DefaultAbsenceType.A_242H7, DefaultAbsenceType.A_242);
      addAutomaticallyRelax(person, abSit, DefaultAbsenceType.A_233H7, DefaultAbsenceType.A_233);
      addAutomaticallyRelax(person, abSit, DefaultAbsenceType.A_253H7, DefaultAbsenceType.A_253);
      addAutomaticallyRelax(person, abSit, DefaultAbsenceType.A_243H7, DefaultAbsenceType.A_243);
    }
  }
  
  private void addAutomaticallyRelax(Person person, AbsenceSituation absenceSituation, 
      DefaultAbsenceType missing, DefaultAbsenceType equivalent) {
    
    Set<LocalDate> toAddAuto = absenceSituation.toAddAutomatically.get(missing.certificationCode);
    if (toAddAuto == null) {
      return;
    }
    Set<LocalDate> relaxedDates = Sets.newHashSet();
    Set<Absence> relaxedAbsences = Sets.newHashSet();
    for (LocalDate date : toAddAuto) {
      List<Absence> absences = absenceComponentDao
          .findAbsences(person, date, equivalent.getCode()); 
      if (!absences.isEmpty()) {
        relaxedDates.add(date);
        relaxedAbsences.addAll(absences);
      }
    }
    putDates(absenceSituation.datesPerCodeOk, missing.certificationCode, relaxedDates);
    removeDates(absenceSituation.toAddAutomatically, missing.certificationCode, relaxedDates);
    absenceSituation.notPresent.removeAll(relaxedAbsences);
    absenceSituation.notPresentSent.removeAll(relaxedAbsences);
  }
  
  /**
   * Le assenze non presenti in attestati dei mesi non successivi l'ultimo caricamento le sposto
   * nella lista notPresentSent (da rimuovere).
   */
  private void patchSentCertifications(List<AbsenceSituation> situations, IWrapperPerson wrPerson) {
    
    if (!wrPerson.lastUpload().isPresent()) {
      return;
    }
    YearMonth lastUpload = wrPerson.lastUpload().get();
    for (AbsenceSituation absenceSituation : situations) {
      for (Absence absence : absenceSituation.notPresent) {
        if (!new YearMonth(absence.getAbsenceDate()).isAfter(lastUpload)) {
          absenceSituation.notPresentSent.add(absence);
        }
      }
      for (Absence absence : absenceSituation.notPresentSent) {
        absenceSituation.notPresent.remove(absence);
      }
    }
    
  }
  
  /**
   * Le assenze mancanti rispetto ad attestati da persistere.
   *
   * @param person person
   * @param year year
   * @return list
   */
  public List<Absence> absencesToPersist(Person person, int year) {

    CertificationYearSituation situation = buildCertificationYearSituation(person, year, false);
    
    //patch su inizializzazioni
    patchPostPartumInit(person, situation.certificationMap);
    
    List<Absence> absenceToPersist = Lists.newArrayList();
    
    JustifiedType allDay = absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.all_day);
    JustifiedType specified = absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.specified_minutes);

    for (AbsenceSituation absenceSituation : situation.absenceSituations) {
      for (String code : absenceSituation.toAddAutomatically.keySet()) {
        Optional<AbsenceType> type = absenceComponentDao.absenceTypeByCode(code);
        if (!type.isPresent()) {
          log.debug("Un codice utilizzato su attestati non è presente su ePAS {}", code);
          continue;
        }

        for (LocalDate date : absenceSituation.toAddAutomatically.get(code)) {
          
          AbsenceType aux = type.get();
          
          //1) I congedi parentali completamento (li faccio diventare normali)
          if (code.equals(DefaultAbsenceType.A_23H7.getCode())) {
            aux = absenceComponentDao.absenceTypeByCode(DefaultAbsenceType.A_23.getCode()).get();
          }
          if (code.equals(DefaultAbsenceType.A_232H7.getCode())) {
            aux = absenceComponentDao.absenceTypeByCode(DefaultAbsenceType.A_232.getCode()).get();
          }
          if (code.equals(DefaultAbsenceType.A_233H7.getCode())) {
            aux = absenceComponentDao.absenceTypeByCode(DefaultAbsenceType.A_233.getCode()).get();
          }
          if (code.equals(DefaultAbsenceType.A_25H7.getCode())) {
            aux = absenceComponentDao.absenceTypeByCode(DefaultAbsenceType.A_25.getCode()).get();
          }
          if (code.equals(DefaultAbsenceType.A_252H7.getCode())) {
            aux = absenceComponentDao.absenceTypeByCode(DefaultAbsenceType.A_252.getCode()).get();
          }
          if (code.equals(DefaultAbsenceType.A_253H7.getCode())) {
            aux = absenceComponentDao.absenceTypeByCode(DefaultAbsenceType.A_253.getCode()).get();
          }
          if (code.equals(DefaultAbsenceType.A_24H7.getCode())) {
            aux = absenceComponentDao.absenceTypeByCode(DefaultAbsenceType.A_24.getCode()).get();
          }
          if (code.equals(DefaultAbsenceType.A_242H7.getCode())) {
            aux = absenceComponentDao.absenceTypeByCode(DefaultAbsenceType.A_242.getCode()).get();
          }
          if (code.equals(DefaultAbsenceType.A_243H7.getCode())) {
            aux = absenceComponentDao.absenceTypeByCode(DefaultAbsenceType.A_243.getCode()).get();
          }
          if (code.equals(DefaultAbsenceType.A_25PH7.getCode())) {
            aux = absenceComponentDao.absenceTypeByCode(DefaultAbsenceType.A_25P.getCode()).get();
          }
          if (!aux.equals(type.get())) {
            absenceToPersist.addAll(absenceService.forceInsert(
                person, date, null, aux, allDay, null, null).absencesToPersist);
            continue;
          }

          //2) I 661H* li faccio diventare 661M
          if (code.equals(DefaultAbsenceType.A_661H1.getCode()) 
              || code.equals(DefaultAbsenceType.A_661H2.getCode())
              || code.equals(DefaultAbsenceType.A_661H3.getCode())
              || code.equals(DefaultAbsenceType.A_661H4.getCode())
              || code.equals(DefaultAbsenceType.A_661H5.getCode())
              || code.equals(DefaultAbsenceType.A_661H6.getCode())
              || code.equals(DefaultAbsenceType.A_661H7.getCode())
              || code.equals(DefaultAbsenceType.A_661H8.getCode())
              || code.equals(DefaultAbsenceType.A_661H9.getCode())) {
            aux = absenceComponentDao.absenceTypeByCode(DefaultAbsenceType.A_661MO.getCode()).get();
            if (aux.isExpired(date)) {
              aux = 
                  absenceComponentDao.absenceTypeByCode(DefaultAbsenceType.A_661M.getCode()).get();
            }
          }
          if (!aux.equals(type.get())) {
            absenceToPersist.addAll(absenceService.forceInsert(person, date, null, 
                aux, specified, type.get().getReplacingTime() / 60, 0).absencesToPersist);
            continue;
          } 

          //2) I 631H* li faccio diventare 631M
          if (code.equals(DefaultAbsenceType.A_631H1.getCode()) 
              || code.equals(DefaultAbsenceType.A_631H2.getCode())
              || code.equals(DefaultAbsenceType.A_631H3.getCode())
              || code.equals(DefaultAbsenceType.A_631H4.getCode())
              || code.equals(DefaultAbsenceType.A_631H5.getCode())
              || code.equals(DefaultAbsenceType.A_631H6.getCode())) {
            aux = absenceComponentDao.absenceTypeByCode(DefaultAbsenceType.A_631M.getCode()).get();
          }
          if (!aux.equals(type.get())) {
            absenceToPersist.addAll(absenceService.forceInsert(person, date, null, 
                aux, specified, type.get().getReplacingTime() / 60, 0).absencesToPersist);
            continue;
          } 

          //3) I 18H* 19H* 182H* li faccio diventare 18M, 19M, 182M
          if (code.equals(DefaultAbsenceType.A_18H1.getCode()) 
              || code.equals(DefaultAbsenceType.A_18H2.getCode())
              || code.equals(DefaultAbsenceType.A_18H3.getCode())
              || code.equals(DefaultAbsenceType.A_18H4.getCode())
              || code.equals(DefaultAbsenceType.A_18H5.getCode())
              || code.equals(DefaultAbsenceType.A_18H6.getCode())
              || code.equals(DefaultAbsenceType.A_18H7.getCode())
              || code.equals(DefaultAbsenceType.A_18H8.getCode())
              || code.equals(DefaultAbsenceType.A_18H9.getCode())) {
            aux = absenceComponentDao.absenceTypeByCode(DefaultAbsenceType.A_18M.getCode()).get();
          }
          if (code.equals(DefaultAbsenceType.A_182H1.getCode()) 
              || code.equals(DefaultAbsenceType.A_182H2.getCode())
              || code.equals(DefaultAbsenceType.A_182H3.getCode())
              || code.equals(DefaultAbsenceType.A_182H4.getCode())
              || code.equals(DefaultAbsenceType.A_182H5.getCode())
              || code.equals(DefaultAbsenceType.A_182H6.getCode())
              || code.equals(DefaultAbsenceType.A_182H7.getCode())
              || code.equals(DefaultAbsenceType.A_182H8.getCode())
              || code.equals(DefaultAbsenceType.A_182H9.getCode())) {
            aux = absenceComponentDao.absenceTypeByCode(DefaultAbsenceType.A_182M.getCode()).get();
          }
          if (code.equals(DefaultAbsenceType.A_19H1.getCode()) 
              || code.equals(DefaultAbsenceType.A_19H2.getCode())
              || code.equals(DefaultAbsenceType.A_19H3.getCode())
              || code.equals(DefaultAbsenceType.A_19H4.getCode())
              || code.equals(DefaultAbsenceType.A_19H5.getCode())
              || code.equals(DefaultAbsenceType.A_19H6.getCode())
              || code.equals(DefaultAbsenceType.A_19H7.getCode())
              || code.equals(DefaultAbsenceType.A_19H8.getCode())
              || code.equals(DefaultAbsenceType.A_19H9.getCode())) {
            aux = absenceComponentDao.absenceTypeByCode(DefaultAbsenceType.A_19M.getCode()).get();
          }
          if (!aux.equals(type.get())) {
            absenceToPersist.addAll(absenceService.forceInsert(person, date, null, 
                aux, specified, type.get().getReplacingTime() / 60, 0).absencesToPersist);
            continue;
          } 

          
          //Gli altri li inserisco senza paura 
          // (a patto che il tipo sia allDay o absence_type_minutes)
          if (type.get().getJustifiedTypesPermitted().size() != 1) {
            log.debug("Impossibile importare una assenza senza justified univoco o definito {}", 
                type.get().getCode());
            continue;
          }
          JustifiedType justifiedType = type.get().getJustifiedTypesPermitted().iterator().next();
          if (justifiedType.getName().equals(JustifiedTypeName.all_day)) {
            absenceToPersist.addAll(absenceService.forceInsert(person, date, null, 
                type.get(), justifiedType, 0, 0).absencesToPersist); 
            continue;
          }
          if (justifiedType.getName().equals(JustifiedTypeName.absence_type_minutes)) {
            int hour = type.get().getJustifiedTime() / 60;
            int minute = type.get().getJustifiedTime() % 60;
            absenceToPersist.addAll(absenceService.forceInsert(person, date, null, 
                type.get(), justifiedType, hour, minute).absencesToPersist); 
            continue;
          }
          if (justifiedType.getName().equals(JustifiedTypeName.complete_day_and_add_overtime)) {
            absenceToPersist.addAll(absenceService.forceInsert(person, date, null, 
                type.get(), justifiedType, 0, 0).absencesToPersist);
            continue;
          }
        }
      }
    }
    return absenceToPersist;
  }
  
  /**
   * Calcola la comparazione con i codici in attestati.
   */
  public CodeComparation computeCodeComparation() {
    
    CodeComparation codeComparation = new CodeComparation();

    try {
      //Codici di assenza in attestati
      Map<String, CodiceAssenza> attestatiAbsenceCodes = certificationService.absenceCodes();
      if (attestatiAbsenceCodes.isEmpty()) {
        log.info("Impossibile accedere ai codici in attestati");
        return null;
      }
      //Tasformazione in superCodes
      for (CodiceAssenza codiceAssenza : attestatiAbsenceCodes.values()) {
        codeComparation.putCodiceAssenza(codiceAssenza);
      }
    } catch (Exception ex) {
      return null;
    }

    //Codici di assenza epas
    List<AbsenceType> absenceTypes = AbsenceType.findAll();
    //Tasformazione in superCodes
    for (AbsenceType absenceType : absenceTypes) {
      codeComparation.putAbsenceType(absenceType);
    }
    
    //Tutte le assenze epas
    List<Absence> absences = Absence.findAll();
    //Inserimento in superCodes
    for (Absence absence : absences) {
      codeComparation.putAbsence(absence);
    }
   
    
    codeComparation.setOnlyAttestati();
    codeComparation.setOnlyEpas();
    codeComparation.setBoth();
    
    return codeComparation;
  }

}