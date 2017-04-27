package manager.services.absences;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import dao.absences.AbsenceComponentDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;

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

import models.Certification;
import models.Person;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.GroupAbsenceType;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.definitions.DefaultAbsenceType;
import models.absences.definitions.DefaultGroup;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

/**
 * Servizi di comparazione fra assenze epas e assenze attestati.
 * @author alessandro
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
   * Situazione assenze epas/attestati.
   * @param person persona
   * @param year anno
   * @return situazione
   */
  public CertificationYearSituation buildCertificationYearSituation(Person person, int year) {
    
    CruscottoDipendente cruscottoCurrent = null;
    CruscottoDipendente cruscottoPrev = null;
    
    try {
      cruscottoCurrent = certificationService.getCruscottoDipendente(person, year);
    } catch (Exception ex) {
      log.debug("Impossibile prelevare il cruscotto di {} per l'anno {}", person.fullName(), year);
      return null;
    }
    try {
      cruscottoPrev = certificationService.getCruscottoDipendente(person, year - 1);
    } catch (Exception ex) {
      log.debug("Impossibile prelevare il cruscotto "
          + "anno precedente di {} per l'anno {}", person.fullName(), year);
      cruscottoPrev = null;
    }
    
    CertificationYearSituation situation = new CertificationYearSituation();
    
    situation.person = person;
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
    from = new LocalDate(LocalDate.now().getYear(), 1 ,1);
    to = new LocalDate(LocalDate.now().getYear(), 12 ,31);
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
    
    patchSentCertifications(situation.absenceSituations, wrPerson);
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
    
    AbsenceSituation absenceSituation = new AbsenceSituation(person, situationType);
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
  
  private boolean absenceIsInEpas(Person person, String code, LocalDate date) {
    
    Set<AbsenceType> certificationCodes = Sets.newHashSet();
    
    Optional<AbsenceType> absenceType = absenceComponentDao.absenceTypeByCode(code);
    if (!absenceType.isPresent() 
        || (absenceType.get().certificateCode != null 
        && !absenceType.get().certificateCode.equalsIgnoreCase(code))) {
      
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
        wrPerson.getCurrentContract().get(), year, vacationGroup, Optional.absent(), false, null);
    
    //Situazione ferie anno selezionato
    AbsenceSituation vacationYear = new AbsenceSituation(wrPerson.getValue(), 
        AbsenceSituationType.FERIE_ANNO_CORRENTE);
    
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
    AbsenceSituation vacationPreviousYear = new AbsenceSituation(wrPerson.getValue(), 
        AbsenceSituationType.FERIE_ANNO_PRECEDENTE);
    
    putDatesVacation(vacationPreviousYear.datesPerCodeOk, mapInEpas, code32, year - 1);
    putDatesVacation(vacationPreviousYear.datesPerCodeOk, mapInEpas, code31, year);
    putDatesVacation(vacationPreviousYear.datesPerCodeOk, mapInEpas ,code37, year);
    putDatesVacation(vacationPreviousYear.toAddAutomatically, mapNotInEpas, code32,  year - 1);
    putDatesVacation(vacationPreviousYear.toAddAutomatically, mapNotInEpas, code31,  year);
    putDatesVacation(vacationPreviousYear.toAddAutomatically, mapNotInEpas,code37,  year);
    if (vacationSituation.lastYear != null) {
      vacationPreviousYear.notPresent = absenceNotInAttestati(
          vacationSituation.lastYear.absencesUsed(),
          vacationPreviousYear.datesPerCodeOk, Lists.newArrayList(code32, code31, code37),
          Optional.absent(), Optional.absent());
    }
    
    //Situazione permessi legge anno selezionato
    AbsenceSituation permissions = new AbsenceSituation(wrPerson.getValue(), 
        AbsenceSituationType.PERMESSI_LEGGE);
    
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
    Set<LocalDate> dates = absenceSituation
        .toAddAutomatically.get(DefaultAbsenceType.A_91.getCode()); 
    if (dates == null || dates.isEmpty()) {
      return;
    }
    LocalDate sourceDate = wrPerson.getCurrentContract().get().sourceDateResidual;
    LocalDate beginContract = wrPerson.getCurrentContract().get().beginDate;
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
   * @param person person
   * @param year year
   * @return list
   */
  public List<Absence> absencesToPersist(Person person, int year) {

    CertificationYearSituation situation = buildCertificationYearSituation(person, year);
    
    List<Absence> absenceToPersist = Lists.newArrayList();
    
    JustifiedType allDay = absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.all_day);
    JustifiedType specified = absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.specified_minutes);

    for (AbsenceSituation absenceSituation : situation.absenceSituations) {
      for (String code : absenceSituation.toAddAutomatically.keySet()) {
        Optional<AbsenceType> type = absenceComponentDao.absenceTypeByCode(code);
        if (!type.isPresent()) {
          log.info("Un codice utilizzato su attestati non è presente su ePAS {}", code);
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
            if (absenceComponentDao.findAbsences(situation.person, date, aux.code).isEmpty()) {
              absenceService.forceInsert(situation.person, date, null, aux, allDay, null, null);
            } else {
              log.info("Assenza presente {} {} {}", situation.person.fullName(), date, aux.code);
            }
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
            aux = absenceComponentDao.absenceTypeByCode(DefaultAbsenceType.A_661M.getCode()).get();
          }
          if (!aux.equals(type.get())) {
            if (absenceComponentDao.findAbsences(situation.person, date, aux.code).isEmpty()) {
              absenceToPersist.addAll(absenceService.forceInsert(situation.person, date, null, 
                  aux, specified, type.get().replacingTime / 60, 0).absencesToPersist);
            } else {
              log.info("Assenza presente {} {} {}", situation.person.fullName(), date, aux.code);
            }
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
            if (absenceComponentDao.findAbsences(situation.person, date, aux.code).isEmpty()) {
              absenceToPersist.addAll(absenceService.forceInsert(situation.person, date, null, 
                  aux, specified, type.get().replacingTime / 60, 0).absencesToPersist);
            } else {
              log.info("Assenza presente {} {} {}", situation.person.fullName(), date, aux.code);
            }
            continue;
          } 
          
          //7) 89 (diritto allo studio). Converto i completamenti in assenze a minuti.
          if (code.equals(DefaultAbsenceType.A_89.getCode())) {
            //TODO: working time day, ricordarsi che è stata importata da attestati.
            //all'inizio ogni dipendente ha il tipo orario 7:12, ma potrebbe essere ridotta
            //quando verrà impostato l'orario corretto.
            aux = absenceComponentDao.absenceTypeByCode(DefaultAbsenceType.A_89M.getCode()).get();
            if (absenceComponentDao.findAbsences(situation.person, date, aux.code).isEmpty()) {
              absenceToPersist.addAll(absenceService.forceInsert(situation.person, date, null, 
                  aux, specified, 432, 0).absencesToPersist);
            } else {
              log.info("Assenza presente {} {} {}", situation.person.fullName(), date, aux.code);
            }
            continue;
          }
          
          //8) 09 (visita medica). Stesso ragionamento.
          if (code.equals(DefaultAbsenceType.A_89.getCode())) {
            //TODO: vedi 89
            aux = absenceComponentDao.absenceTypeByCode(DefaultAbsenceType.A_89M.getCode()).get();
            if (absenceComponentDao.findAbsences(situation.person, date, aux.code).isEmpty()) {
              absenceToPersist.addAll(absenceService.forceInsert(situation.person, date, null, 
                  aux, specified, 432, 0).absencesToPersist); 
            } else {
              log.info("Assenza presente {} {} {}", situation.person.fullName(), date, aux.code);
            }
            continue;
          }
          
          //Gli altri li inserisco senza paura 
          // (a patto che il tipo sia allDay o absence_type_minutes)
          if (type.get().justifiedTypesPermitted.size() != 1) {
            log.info("Impossibile importare una assenza senza justified univoco o definito {}", 
                type.get().code);
            continue;
          }
          JustifiedType justifiedType = type.get().justifiedTypesPermitted.iterator().next();
          if (justifiedType.name.equals(JustifiedTypeName.all_day)) {
            if (absenceComponentDao
                .findAbsences(situation.person, date, type.get().code).isEmpty()) {
              absenceToPersist.addAll(absenceService.forceInsert(situation.person, date, null, 
                  type.get(), justifiedType, 0, 0).absencesToPersist); 
            } else {
              log.info("Assenza presente {} {} {}", situation.person.fullName(), date, aux.code);
            }
            continue;
          }
          if (justifiedType.name.equals(JustifiedTypeName.absence_type_minutes)) {
            int hour = type.get().justifiedTime / 60;
            int minute = type.get().justifiedTime % 60;
            if (absenceComponentDao
                .findAbsences(situation.person, date, type.get().code).isEmpty()) {
              absenceToPersist.addAll(absenceService.forceInsert(situation.person, date, null, 
                  type.get(), justifiedType, hour, minute).absencesToPersist); 
            } else {
              log.info("Assenza presente {} {} {}", situation.person.fullName(), date, aux.code);
            }
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
