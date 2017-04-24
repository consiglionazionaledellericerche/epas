package manager.services.absences;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
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
import manager.services.absences.certifications.CertificationYearSituation.AbsenceImportType;
import manager.services.absences.certifications.CertificationYearSituation.AbsenceSituation;
import manager.services.absences.certifications.CodeComparation;
import manager.services.absences.model.VacationSituation;

import models.Person;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.GroupAbsenceType;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.definitions.DefaultAbsenceType;
import models.absences.definitions.DefaultGroup;

import org.joda.time.LocalDate;

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
  
  
  
  /**
   * Situazione assenze epas/attestati.
   * @param person persona
   * @param year anno
   * @return situazione
   */
  public CertificationYearSituation buildCertificationYearSituation(Person person, int year) {
    
    CruscottoDipendente cruscotto = null;
    
    try {
      cruscotto = certificationService.getCruscottoDipendente(person, year);
    } catch (Exception ex) {
      log.info("Impossibile prelevare il cruscotto di {} per l'anno {}", person.fullName(), year);
      return null;
    }
    
    CertificationYearSituation situation = new CertificationYearSituation();
    
    situation.person = person;
    situation.year = cruscotto.annoSituazione;
    
    IWrapperPerson wrPerson = wrapperFactory.create(person);
    
    situation.beginDate = new LocalDate(cruscotto.dipendente.dataAssunzione);
    if (cruscotto.dipendente.dataCessazione != null) {
      situation.endDate = new LocalDate(cruscotto.dipendente.dataCessazione);
    }
    
    for (SituazioneDipendenteAssenze sda : cruscotto.situazioneDipendenteAssenze) {
      AbsenceSituation absenceSituation = buildAbsenceSituation(wrPerson, sda, year);
      if (absenceSituation != null) {
        situation.absenceSituations.add(absenceSituation);
      }
    }
    
    for (SituazioneParametriControllo spa : cruscotto.situazioneParametriControllo) {
      AbsenceSituation absenceSituation = buildControlSituation(wrPerson, spa);
      if (absenceSituation != null) {
        situation.absenceSituations.add(absenceSituation);
      }
    }
   
    return situation;
  }
  
  /**
   * Costruisce la situazione assenze dalla struttura dipendente assenze.
   * 1) 661
   * 2) Ferie anno passato
   */
  private AbsenceSituation buildAbsenceSituation(IWrapperPerson wrPerson, 
      SituazioneDipendenteAssenze sda, int year) {

    //Il tipo di situazione
    AbsenceImportType absenceImportType = AbsenceImportType.TIPO_GENERICO;
    
    //Ignorati perchè prelevati da SituazioneParametriControllo
    for (AbsenceImportType importType : AbsenceImportType.values()) {
      if (importType.absenceMatchExclusions == null 
          || importType.absenceMatchExclusions.isEmpty()) {
        continue;
      }
      for (String codeExclusion : importType.absenceMatchExclusions) {
        if (sda.codice.codice.equals(codeExclusion)) {
          return null;
        }
      }
    }

    //Selezione del tipo di import type
    for (AbsenceImportType type : AbsenceImportType.values()) {
      if (type.absenceMatchPattern == null) {
        continue;
      }
      if (sda.codice.codice.equals(type.absenceMatchPattern)) {
        absenceImportType = type;
        break;
      }
    }
    
    AbsenceSituation absenceSituation = 
        new AbsenceSituation(wrPerson.getValue(), absenceImportType);
    
    dispatchDates(absenceSituation, wrPerson, sda.codice.codice, sda.codeDates());
    
    toRemove(wrPerson, absenceSituation, year);
    
    
    
    //totali residui
    if (absenceSituation.type.equals(AbsenceImportType.FERIE_ANNO_CORRENTE)
        || absenceSituation.type.equals(AbsenceImportType.PERMESSI_LEGGE)) {
      absenceSituation.totalUsable = sda.qtLimiteConsentito;
      absenceSituation.totalResidual = sda.qtResiduaOreGiorni;
    }
    
    return absenceSituation;
  }
  
  
  
  /**
   * Costruisce la situazione assenze dalla struttura parametri di controllo.
   * 1) 661
   * 2) Ferie anno passato
   */
  private AbsenceSituation buildControlSituation(IWrapperPerson wrPerson, 
      SituazioneParametriControllo spa) {
    
    //Il tipo di situazione
    AbsenceImportType absenceImportType = null;
    for (AbsenceImportType type : AbsenceImportType.values()) {
      if (type.controlMatchPattern == null) {
        continue;
      }
      if (spa.descrizione.equals(type.controlMatchPattern)) {
        absenceImportType = type;
        break;
      }
    }
    if (absenceImportType == null) {
      return null;  
    }
    
    AbsenceSituation absenceSituation = 
        new AbsenceSituation(wrPerson.getValue(), absenceImportType);
    
    Map<String, List<LocalDate>> codesDates = spa.codesDates();
    for (String code : codesDates.keySet()) {
      dispatchDates(absenceSituation, wrPerson, code, codesDates.get(code));  
    }
    
    //TODO: absences To remove
    
    //totali residui
    if (absenceSituation.type.equals(AbsenceImportType.FERIE_ANNO_PRECEDENTE) 
        || absenceSituation.type.equals(AbsenceImportType.PERMESSO_PERSONALE_661)) {
      absenceSituation.totalUsable = spa.qtLimiteConsentito;
      absenceSituation.totalResidual = spa.qtResiduaOreGiorni;
    }
    
    return absenceSituation;
  }
  
  /**
   * Smista le assenze in attestati nelle liste ok, toAddManually, toAdAutomatically.
   */
  private AbsenceSituation dispatchDates(AbsenceSituation absenceSituation, 
      IWrapperPerson wrPerson, String code, List<LocalDate> dates) {

    List<LocalDate> datesOk = Lists.newArrayList();
    List<LocalDate> datesToAddAutomatically = Lists.newArrayList();
    List<LocalDate> datesToAddManually = Lists.newArrayList();
    
    LocalDate sourceDateResidual = wrPerson.getCurrentContract().get().sourceDateResidual;
    
    for (LocalDate date : dates) {
      
      //caso particolare riposo compensativo
      if (absenceSituation.type.equals(AbsenceImportType.RIPOSO_COMPENSATIVO)) {
        boolean isCompensatoryPresent = isAbsencePresent(absenceSituation, code, date);

        //Riposo compensativo pre inizializzazione
        if (sourceDateResidual != null && !date.isAfter(sourceDateResidual)) {
          if (isCompensatoryPresent) {
            datesOk.add(date);
          } else {
            datesToAddAutomatically.add(date);
          }
          continue;  
        } 
        
        //Giorni post inizializzazione
        if (isCompensatoryPresent) {
          datesOk.add(date);
        } else {
          datesToAddManually.add(date);
        }
        continue;
      }
      
      //caso generico
      if (isAbsencePresent(absenceSituation, code, date)) {
        datesOk.add(date);
      } else {
        datesToAddAutomatically.add(date);
      }
    }
    
    absenceSituation.datesPerCodeOk.put(code, datesOk);
    absenceSituation.datesPerCodeToAddAutomatically.put(code, datesToAddAutomatically);
    absenceSituation.datesPerCodeToAddManually.put(code, datesToAddManually);
    
    return absenceSituation;
  }
  
  /**
   * Se l'assenza è presente su ePAS.
   * @param code codice in attestati
   * @param date data
   * @return esito
   */
  private boolean isAbsencePresent(AbsenceSituation situation, String code, LocalDate date) {
    
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
      absences.addAll(absenceComponentDao.orderedAbsences(situation.person, date, date, 
          Sets.newHashSet(certificationType)));
    }

    //Dovrebbe essere unica
    if (absences.isEmpty()) {
      return false;
    }
    
    if (absences.size() > 1) {
      situation.duplicatedAbsences.addAll(absences);
    }
    
    return true;
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
      for (String code : absenceSituation.datesPerCodeToAddAutomatically.keySet()) {
        Optional<AbsenceType> type = absenceComponentDao.absenceTypeByCode(code);
        if (!type.isPresent()) {
          log.info("Un codice utilizzato su attestati non è presente su ePAS {}", code);
          continue;
        }

        for (LocalDate date : absenceSituation.datesPerCodeToAddAutomatically.get(code)) {
          
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
  
  //le assenze epas significative non presenti in attestati
  // giorni ferie e permessi dopo l'inizializzazione
  // giorni di astensione fac. dopo l'inizializzazione
  // giorni di riduce ferie e permessi (non considerare inizializzazioni, servono gg specifici) 
  // giorni di malattia figli
  // giorni di malattia
  private AbsenceSituation toRemove(IWrapperPerson wrPerson, AbsenceSituation absenceSituation, 
      int year) {

    // giorni ferie e permessi dopo l'inizializzazione
    if (absenceSituation.type.equals(AbsenceImportType.FERIE_ANNO_CORRENTE)) {
      
      GroupAbsenceType vacationGroup = absenceComponentDao
          .groupAbsenceTypeByName(DefaultGroup.FERIE_CNR.name()).get();
      
      //costruire la situazione ferie year
      VacationSituation vacationSituation = absenceService
          .buildVacationSituation(wrPerson.getCurrentContract().get(), year, vacationGroup, 
              Optional.absent(), false, null);

      absenceSituation.absencesToRemove = absencesNotPresent(absenceSituation.datesPerCodeOk, 
          vacationSituation.currentYear.absencesUsed(), 
          Optional.fromNullable(wrPerson.getCurrentContract().get().sourceDateResidual)); 
    }

    
    return absenceSituation;
    
    
  }
  
  /**
   * Controlla che le assenze absences siano presenti in attestati con certification code.
   * Non controlla le assenze precedenti a fromDate.
   */
  private List<Absence> absencesNotPresent(Map<String, List<LocalDate>> datesPerCodes, 
      List<Absence> absences, Optional<LocalDate> fromDate) {
    
    List<Absence> absencesNotPresent = Lists.newArrayList();
    for (Absence absence : absences) {
      if (fromDate.isPresent() && absence.getAbsenceDate().isBefore(fromDate.get())) {
        continue;
      }
      List<LocalDate> dates = datesPerCodes.get(absence.absenceType.certificateCode);
      if (dates == null) {
        absencesNotPresent.add(absence);
        continue;
      }
      boolean finded = false;
      for (LocalDate date : dates) {
        if (date.isEqual(absence.getAbsenceDate())) {
          finded = true;
          break;
        }
      }
      if (!finded) {
        absencesNotPresent.add(absence);
      }
    }
    return absencesNotPresent;
    
  }
  
}
