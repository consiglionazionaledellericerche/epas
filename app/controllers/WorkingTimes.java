package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import dao.ContractDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.WorkingTimeTypeDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperWorkingTimeType;
import dao.wrapper.function.WrapperModelFunctionFactory;

import helpers.ValidationHelper;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import manager.ContractManager;
import manager.PeriodManager;
import manager.WorkingTimeTypeManager;

import models.Contract;
import models.ContractWorkingTimeType;
import models.Office;
import models.Person;
import models.WorkingTimeType;
import models.WorkingTimeTypeDay;
import models.dto.HorizontalWorkingTime;
import models.dto.VerticalWorkingTime;
import models.enumerate.WorkingTimeTypePattern;

import org.joda.time.LocalDate;

import play.cache.Cache;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;

import security.SecurityRules;

@With({Resecure.class})
@Slf4j
public class WorkingTimes extends Controller {

  private static final String VERTICAL_WORKING_TIME_STEP = "vwt";
  public static final int NUMBER_OF_DAYS = 7;

  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static SecurityRules rules;
  @Inject
  private static WorkingTimeTypeDao workingTimeTypeDao;
  @Inject
  private static WrapperModelFunctionFactory wrapperFunctionFactory;
  @Inject
  private static ContractDao contractDao;
  @Inject
  private static IWrapperFactory wrapperFactory;
  @Inject
  private static WorkingTimeTypeManager workingTimeTypeManager;
  @Inject
  private static ContractManager contractManager;
  @Inject
  private static PersonDao personDao;
  @Inject
  private static PeriodManager periodManager;

  /**
   * Gestione dei tipi orario.
   *
   * @param officeId sede
   */
  public static void manageWorkingTime(Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);

    List<IWrapperWorkingTimeType> wttDefault = FluentIterable
        .from(workingTimeTypeDao.getDefaultWorkingTimeType())
        .transform(wrapperFunctionFactory.workingTimeType()).toList();

    render(wttDefault, office);
  }

  /**
   * Gestione dei tipi orario particolari.
   *
   * @param officeId sede
   */
  public static void manageOfficeWorkingTime(Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);

    List<IWrapperWorkingTimeType> wttAllowed = FluentIterable
        .from(office.workingTimeType)
        .transform(wrapperFunctionFactory.workingTimeType()).toList();

    List<IWrapperWorkingTimeType> wttAllowedEnabled = Lists.newArrayList();
    List<IWrapperWorkingTimeType> wttAllowedDisabled = Lists.newArrayList();
    for (IWrapperWorkingTimeType wtt : wttAllowed) {
      if (wtt.getValue().disabled) {
        wttAllowedDisabled.add(wtt);
      } else {
        wttAllowedEnabled.add(wtt);
      }
    }

    render(wttAllowedEnabled, wttAllowedDisabled, office);
  }


  /**
   * I contratti attivi che per quella sede hanno quel tipo orario.
   *
   * @param wttId    orario
   * @param officeId sede
   */
  public static void showContract(Long wttId, Long officeId) {

    WorkingTimeType wtt = workingTimeTypeDao.getWorkingTimeTypeById(wttId);
    notFoundIfNull(wtt);
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    rules.checkIfPermitted(wtt.office);
    rules.checkIfPermitted(office);

    List<Contract> contractList = wrapperFactory.create(wtt).getAssociatedActiveContract(office);

    render(wtt, contractList, office);

  }

  /**
   * Mostra i periodi con quel tipo orario appartenenti a contratti attualmente attivi.
   * @param wttId tipo orario
   * @param officeId sede
   */
  public static void showContractWorkingTimeType(Long wttId, Long officeId) {

    WorkingTimeType wtt = workingTimeTypeDao.getWorkingTimeTypeById(wttId);
    notFoundIfNull(wtt);
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    rules.checkIfPermitted(wtt.office);
    rules.checkIfPermitted(office);

    IWrapperWorkingTimeType wwtt = wrapperFactory.create(wtt);

    List<ContractWorkingTimeType> cwttList = wwtt.getAssociatedPeriodInActiveContract(office);

    render(wtt, cwttList, office);

  }
  
  /**
   * Inserimento delle informazioni di base tipo orario.
   * @param officeId id ufficio
   * @param compute controllo degli step
   * @param name nome identificativo dell'orario di lavoro
   * @param workingTimeTypePattern tipo di orario di lavoro (orizzontale/verticale).
   */
  public static void insertWorkingTimeBaseInformation(Long officeId, boolean compute,
      String name, WorkingTimeTypePattern workingTimeTypePattern) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);

    if (!compute) {
      workingTimeTypePattern = WorkingTimeTypePattern.HORIZONTAL;
      render(office, workingTimeTypePattern);
    }

    //Controllo unicità
    if (name == null || name.isEmpty()) {
      Validation.addError("name", "non può essere vuoto");
    } else {
      WorkingTimeType wtt = workingTimeTypeDao.workingTypeTypeByDescription(name,
          Optional.<Office>absent());
      if (wtt != null) {
        Validation.addError("name", "già presente in archivio.");
      }
    }

    if (Validation.hasErrors()) {
      render(office, name, workingTimeTypePattern);
    }

    if (workingTimeTypePattern.equals(WorkingTimeTypePattern.HORIZONTAL)) {
      HorizontalWorkingTime horizontalPattern = new HorizontalWorkingTime();
      horizontalPattern.name = name;
      render("@insertWorkingTime", horizontalPattern, office, name);
    } else {
      final String key = VERTICAL_WORKING_TIME_STEP + name + Security.getUser().get().username;
      List<VerticalWorkingTime> vwtProcessedList = processed(key);
      Set<Integer> daysProcessed = dayProcessed(vwtProcessedList);
      int step = 1;
      VerticalWorkingTime vwt = get(vwtProcessedList, step, Optional.<VerticalWorkingTime>absent());

      render("@insertVerticalWorkingTime", office, vwt, name, step, daysProcessed);
    }
  }

  private static VerticalWorkingTime get(List<VerticalWorkingTime> wttList, int step,
      Optional<VerticalWorkingTime> lastInsert) {
    VerticalWorkingTime vwt = null;
    for (VerticalWorkingTime processed : wttList) {
      if (processed.dayOfWeek == step) {
        vwt = processed;
      }
    }
    if (vwt == null) {
      if (lastInsert.isPresent()) {
        vwt = lastInsert.get();
      } else {
        vwt = new VerticalWorkingTime();
      }
    }
    return vwt;
  }

  private static void add(List<VerticalWorkingTime> wttList, VerticalWorkingTime vwt, int step) {
    //rimuovere il vecchio
    VerticalWorkingTime toDelete = null;
    for (VerticalWorkingTime vwtOld : wttList) {
      if (vwtOld.dayOfWeek == step) {
        toDelete = vwtOld;
      }
    }
    if (toDelete != null) {
      wttList.remove(toDelete);
    }
    wttList.add(vwt);
  }

  private static List<VerticalWorkingTime> processed(String key) {
    List<VerticalWorkingTime> vwtProcessedList = Cache.get(key, List.class);
    if (vwtProcessedList == null) {
      vwtProcessedList = Lists.newArrayList();
    }
    return vwtProcessedList;
  }

  private static Set<Integer> dayProcessed(List<VerticalWorkingTime> wttList) {
    Set<Integer> daysProcessed = Sets.newHashSet();
    for (VerticalWorkingTime processed : wttList) {
      daysProcessed.add(processed.dayOfWeek);
    }
    return daysProcessed;
  }
  
  /**
   *
   * @param officeId id Ufficio proprietario
   * @param name nome dell'orario di lavoro
   * @param step numero dello step di creazione dell'orario verticale
   * @param switchDay passaggio da un giorno all'altro in fase di creazione
   * @param submit  booleano per completare la procedura
   * @param vwt Orario di lavoro verticale.
   */
  public static void insertVerticalWorkingTime(Long officeId, @Required String name, int step,
      boolean switchDay, boolean submit, @Valid VerticalWorkingTime vwt) {

    flash.clear();

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);

    final String key = VERTICAL_WORKING_TIME_STEP + name + Security.getUser().get().username;
    List<VerticalWorkingTime> vwtProcessedList = processed(key);
    Set<Integer> daysProcessed = dayProcessed(vwtProcessedList);

    //Caso del cambio giorno ...
    if (switchDay) {
      validation.clear();
      vwt = get(vwtProcessedList, step, Optional.<VerticalWorkingTime>absent());
      render(office, vwt, name, step, daysProcessed);
    }

    //Persistenza ...
    if (submit) {
      // TODO: validatore
      workingTimeTypeManager.saveVerticalWorkingTimeType(vwtProcessedList, office, name);
      flash.success("Il nuovo tipo orario è stato inserito correttamente.");
      manageOfficeWorkingTime(office.id);
    }

    Preconditions.checkNotNull(vwt);
    // Validazione dto
    if (validation.hasErrors()) {
      flash.error("Occorre correggere gli errori riportati.");
      render(office, vwt, name, step, daysProcessed);
    }

    // Next step
    vwt.dayOfWeek = step;
    add(vwtProcessedList, vwt, step);
    Cache.safeAdd(key, vwtProcessedList, "30mn");
    daysProcessed.add(vwt.dayOfWeek);
    if (step < NUMBER_OF_DAYS) {
      step++;
      vwt = get(vwtProcessedList, step, Optional.fromNullable(vwt));
    }
    render(vwt, step, name, office, daysProcessed);


  }

  /**
   * TODO: per la renderizzazione dell'orario verticale prima del salvataggio.
   *
   * @param officeId l'id dell'ufficio in cui si vuole inserire il nuovo orario
   * @param list     la lista dei dto contenente le info per il nuovo orario
   */
  public static void renderVerticalWorkingTime(Long officeId, List<VerticalWorkingTime> list) {
    Office office = officeDao.getOfficeById(officeId);
    render(office, list);
  }

  /**
   * metodo che consente la creazione di un nuovo orario di lavoro orizzontale.
   *
   * @param horizontalPattern il dto contenente le informazioni da persistere
   * @param office            l'ufficio a cui associare l'orario di lavoro
   */
  public static void saveHorizontal(@Valid HorizontalWorkingTime horizontalPattern,
      @Required Office office) {

    // TODO: la creazione dell'orario default ha office null.
    notFoundIfNull(office);

    rules.checkIfPermitted(office);

    WorkingTimeType wtt = workingTimeTypeDao.workingTypeTypeByDescription(
        horizontalPattern.name, Optional.fromNullable(office));
    if (wtt != null) {
      Validation.addError("horizontalPattern.name", "nome già presente", horizontalPattern.name);
    }

    if (Validation.hasErrors()) {
      boolean horizontal = true;
      render("@insertWorkingTime", horizontalPattern, horizontal, office);
    }

    horizontalPattern.buildWorkingTimeType(office);

    flash.success("Orario creato con successo.");

    manageOfficeWorkingTime(office.id);

  }

  /**
   * Salvataggio tipo orario.
   */
  public static void save(@Valid WorkingTimeType wtt, WorkingTimeTypeDay wttd1,
      WorkingTimeTypeDay wttd2, WorkingTimeTypeDay wttd3,
      WorkingTimeTypeDay wttd4, WorkingTimeTypeDay wttd5,
      WorkingTimeTypeDay wttd6, WorkingTimeTypeDay wttd7) {

    if (Validation.hasErrors()) {
      flash.error(ValidationHelper.errorsMessages(Validation.errors()));
      manageWorkingTime(wtt.office.id);
    }


    rules.checkIfPermitted(wtt.office);

    wtt.save();

    workingTimeTypeManager.saveWorkingTimeType(wttd1, wtt, 1);
    workingTimeTypeManager.saveWorkingTimeType(wttd2, wtt, 2);
    workingTimeTypeManager.saveWorkingTimeType(wttd3, wtt, 3);
    workingTimeTypeManager.saveWorkingTimeType(wttd4, wtt, 4);
    workingTimeTypeManager.saveWorkingTimeType(wttd5, wtt, 5);
    workingTimeTypeManager.saveWorkingTimeType(wttd6, wtt, 6);
    workingTimeTypeManager.saveWorkingTimeType(wttd7, wtt, 7);

    flash.success("Il nuovo tipo orario è stato inserito correttamente.");

    manageWorkingTime(wtt.office.id);

  }

  /**
   * Mostra il tipo orario orizzontale.
   * @param wttId tipo orario.
   */
  public static void showHorizontal(Long wttId) {

    WorkingTimeType wtt = workingTimeTypeDao.getWorkingTimeTypeById(wttId);
    notFoundIfNull(wtt);

    // se non è horizontal ho sbagliato action
    Preconditions.checkState(wtt.horizontal);

    if (wtt.office != null) {
      rules.checkIfPermitted(wtt.office);
    }

    HorizontalWorkingTime horizontalPattern = new HorizontalWorkingTime(wtt);

    Office office = wtt.office;

    render(horizontalPattern, office);
  }

  /**
   * Mostra il tipo orario.
   * @param wttId tipo orario
   */
  public static void showWorkingTimeType(Long wttId) {

    WorkingTimeType wtt = workingTimeTypeDao.getWorkingTimeTypeById(wttId);
    if (wtt == null) {

      flash.error("Impossibile caricare il tipo orario specificato. "
          + "Riprovare o effettuare una segnalazione.");
      WorkingTimes.manageWorkingTime(null);
    }

    rules.checkIfPermitted(wtt.office);

    render(wtt);
  }

  /**
   * Elimina un tipo orario (non deve essere associato ad alcun contratto).
   * @param wttId tipo orario
   */
  public static void delete(Long wttId) {

    WorkingTimeType wtt = workingTimeTypeDao.getWorkingTimeTypeById(wttId);
    notFoundIfNull(wtt);
    rules.checkIfPermitted(wtt.office);

    //Prima di cancellare il tipo orario controllo che non sia associato ad alcun contratto
    if (wrapperFactory.create(wtt).getAssociatedContract().size() > 0) {

      flash.error("Impossibile eliminare il tipo orario selezionato perchè "
          + "associato ad almeno un contratto. Operazione annullata");
      WorkingTimes.manageOfficeWorkingTime(wtt.office.id);
    }

    for (WorkingTimeTypeDay wttd : wtt.workingTimeTypeDays) {
      wttd.delete();
    }
    wtt.delete();

    flash.success("Tipo orario eliminato.");
    WorkingTimes.manageOfficeWorkingTime(wtt.office.id);

  }

  /**
   * Abilita/Disabilita il tipo orario.
   *
   * @param wttId tipo orario
   */
  public static void toggleWorkingTimeTypeEnabled(Long wttId) {

    WorkingTimeType wtt = workingTimeTypeDao.getWorkingTimeTypeById(wttId);
    notFoundIfNull(wtt);
    rules.checkIfPermitted(wtt.office);

    IWrapperWorkingTimeType wwtt = wrapperFactory.create(wtt);

    //Prima di disattivarlo controllo che non sia associato ad alcun contratto attivo
    if (wtt.disabled == false && wwtt.getAssociatedActiveContract(wtt.office).size() > 0) {

      flash.error("Impossibile eliminare il tipo orario selezionato perchè "
          + "attualmente associato ad almeno un contratto attivo.");
      manageOfficeWorkingTime(wtt.office.id);
    }

    if (wtt.disabled) {

      wtt.disabled = false;
      wtt.save();
      flash.success("Riattivato correttamente orario di lavoro.");
      manageOfficeWorkingTime(wtt.office.id);
    } else {

      wtt.disabled = true;
      wtt.save();
      flash.success("Disattivato orario di lavoro.");
      manageOfficeWorkingTime(wtt.office.id);
    }

  }


  /**
   * Modale per il cambia orario a tutti.
   * @param wttId tipo orario
   * @param officeId sede
   */
  public static void changeWorkingTimeTypeToAll(Long wttId, Long officeId) {

    WorkingTimeType wtt = workingTimeTypeDao.getWorkingTimeTypeById(wttId);
    notFoundIfNull(wtt);
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);

    List<WorkingTimeType> wttList = workingTimeTypeDao.getEnabledWorkingTimeTypeForOffice(office);
    wttList.remove(wtt);

    render(wtt, wttList, office);
  }

  /**
   * Esegue il cambia orario a tutti.
   * @param wtt vecchio tipo
   * @param wttNew nuovo tipo
   * @param officeId sede
   * @param dateFrom data inizio
   * @param dateTo data fine
   */
  public static void executeChangeWorkingTimeTypeToAll(
      WorkingTimeType wtt, WorkingTimeType wttNew, Long officeId, LocalDate dateFrom,
      LocalDate dateTo) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    if (wttNew == null || !wttNew.isPersistent()) {
      validation.addError("wttNew", "Campo obbligatorio.");
    }
    if (dateFrom == null) {
      validation.addError("dateFrom", "Campo obbligatorio.");
    } else {
      validation.future(dateFrom.toDate(), 
          LocalDate.now().minusMonths(1).dayOfMonth().withMinimumValue().minusDays(1).toDate())
      .key("dateFrom").message("validation.after");
      if (dateTo != null && dateFrom.isAfter(dateTo)) {
        validation.addError("dateTo", "Deve essere sucessivo alla data iniziale");
      }
    }
    if (Validation.hasErrors()) {
      List<WorkingTimeType> wttList = workingTimeTypeDao.getEnabledWorkingTimeTypeForOffice(office);
      wttList.remove(wtt);
      response.status = 400;
      render("@changeWorkingTimeTypeToAll", office, wttList, wtt, wttNew,  
          dateFrom, dateTo);
    }
    
    if (wtt.office != null && wttNew.office != null && !wtt.office.id.equals(wttNew.office.id)) {
      badRequest();
    }

    rules.checkIfPermitted(office);

    DateInterval requestInterval = new DateInterval(dateFrom, dateTo);
    
    for (Person person : personDao.list(Optional.of(office)).list()) {
      for (Contract contract : person.contracts) {
        
        DateInterval contractInterval = DateUtility
            .intervalIntersection(contract.periodInterval(), requestInterval);
        if (contractInterval == null) {
          continue;
        }
        ContractWorkingTimeType cwtt = new ContractWorkingTimeType();
        cwtt.contract = contract;          
        cwtt.beginDate = contractInterval.getBegin();
        if (!DateUtility.isInfinity(contractInterval.getEnd())) {
          cwtt.endDate = contractInterval.getEnd(); //altrimenti null  
        }
        cwtt.workingTimeType = wttNew;
        
        try {
          periodManager.updatePeriods(cwtt, true);
          contract = contractDao.getContractById(contract.id);
          contract.person.refresh();
          contractManager.recomputeContract(contract, Optional.of(dateFrom), false, false);
        } catch (Exception ex) {
          log.error("La situazione dei contract working time type per la persona {} è compromessa,"
              + "ripristinare manualmente la situazione da gestisci contratto, ridefinendo le sue "
              + "date iniziali e finali."); 
        }
      }
    }
    
    //redirect (torno alla modifica dell'orario di partenza)
    if (wtt.office == null) {
      manageWorkingTime(office.id);
    } else {
      manageOfficeWorkingTime(office.id);
    }
  }


}
