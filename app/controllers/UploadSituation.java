package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

import dao.OfficeDao;
import dao.PersonMonthRecapDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperOffice;

import lombok.extern.slf4j.Slf4j;

import manager.UploadSituationManager;
import manager.attestati.old.AttestatiClient;
import manager.attestati.old.AttestatiClient.DipendenteComparedRecap;
import manager.attestati.old.AttestatiClient.SessionAttestati;
import manager.attestati.old.AttestatiException;
import manager.attestati.old.Dipendente;
import manager.attestati.old.RispostaElaboraDati;

import models.CertificatedData;
import models.Office;

import org.apache.commons.io.IOUtils;
import org.joda.time.YearMonth;

import play.Play;
import play.cache.Cache;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

/**
 * Contiene in metodi necessari per l'interazione tra utente, ePAS e sistema centrale del CNR per
 * gli attestati.
 *
 * @author cristian
 */
@Slf4j
@With({Resecure.class, RequestInit.class})
public class UploadSituation extends Controller {

  public static final String LOGIN_RESPONSE_CACHED = "loginResponse";
  public static final String LISTA_DIPENTENTI_CNR_CACHED = "listaDipendentiCnr";

  public static final String FILE_PREFIX = "situazioneMensile";
  public static final String FILE_SUFFIX = ".txt";

  public static final String DEFAULT_URL_TO_PRESENCE = "https://attestati.rm.cnr.it/attestati/";
  public static final String URL_TO_PRESENCE = Play.configuration.getProperty("url_to_presence", DEFAULT_URL_TO_PRESENCE);


  @Inject
  private static SecurityRules rules;
  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static AttestatiClient attestatiClient;
  @Inject
  private static PersonMonthRecapDao personMonthRecapDao;
  @Inject
  private static IWrapperFactory factory;
  @Inject
  private static UploadSituationManager updloadSituationManager;

  /**
   * Tab carica data.
   *
   * @param officeId sede
   */
  public static void uploadData(Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);

    IWrapperOffice wrOffice = factory.create(office);
    Optional<YearMonth> monthToUpload = wrOffice.nextYearMonthToUpload();

    //caricare eventuale sessione già presente
    SessionAttestati sessionAttestati = loadAttestatiLoginCached();

    if (sessionAttestati != null && monthToUpload.isPresent()) {

      int year = sessionAttestati.getYear();
      int month = sessionAttestati.getMonth();

      monthToUpload = Optional.fromNullable(new YearMonth(year, month));

      DipendenteComparedRecap dipendenteComparedRecap = attestatiClient
          .buildComparedLists(office, sessionAttestati);

      IWrapperFactory wrapper = factory;
      render(wrOffice, monthToUpload, sessionAttestati, dipendenteComparedRecap, wrapper);
    }

    render(wrOffice, monthToUpload, sessionAttestati);
  }
  
  /**
   * Modale Cambia il mese e la sede.
   *
   * @param officeId sede
   */
  public static void updateSession(Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);

    IWrapperOffice wrOffice = factory.create(office);
    SessionAttestati sessionAttestati = loadAttestatiLoginCached();

    if (sessionAttestati != null) {
      YearMonth monthToUpload = new YearMonth(sessionAttestati.getYear(),
          sessionAttestati.getMonth());
      render(wrOffice, sessionAttestati, monthToUpload);

    } else {
      //sessione scaduta.
      render(wrOffice);
    }
  }

  /**
   * Tab creazione file.
   *
   * @param officeId sede
   * @param year     anno
   * @param month    mese
   */
  public static void createFile(Long officeId, Integer year, Integer month) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);

    //TODO: costruire la lista degli anni sulla base di tutti gli uffici permessi 
    //e usarla nel template.

    IWrapperOffice wrOffice = factory.create(office);
    Optional<YearMonth> monthToUpload = wrOffice.nextYearMonthToUpload();
    render(wrOffice, monthToUpload);
  }

  /**
   * Tab creazione file.
   *
   * @param office sede
   * @param year   anno
   * @param month  mese
   */
  public static void computeCreateFile(@Valid Office office,
                                       @Required Integer year, @Required Integer month) {

    notFoundIfNull(office);
    rules.checkIfPermitted(office);

    IWrapperOffice wrOffice = factory.create(office);
    if (!validation.hasErrors()) {
      //controllo che il mese sia uploadable
      if (!wrOffice.isYearMonthUploadable(new YearMonth(year, month))) {
        validation.addError("year", "non può essere precedente al primo mese riepilogabile");
        validation.addError("month", "non può essere precedente al primo mese riepilogabile");
      }
    }

    if (validation.hasErrors()) {
      response.status = 400;
      //flash.error(Web.msgHasErrors());
      log.warn("validation errors: {}", validation.errorsMap());
      Optional<YearMonth> monthToUpload = Optional.of(new YearMonth(year, month));
      render("@createFile", wrOffice, monthToUpload);
    }
    
    String body = updloadSituationManager.createFile(office, year, month);
    String fileName = FILE_PREFIX + office.codeId + " - " + year + month + FILE_SUFFIX;
    renderBinary(IOUtils.toInputStream(body), fileName);
  }

  /**
   * Carica i dati sul personale (sia lato CNR sia lato ePAS confrontando le due liste). Se
   * necessario effettua login (in caso di username e password null si cerca la login in cache).
   *
   * @param office            sede
   * @param year              anno
   * @param month             mese
   * @param attestatiLogin    username
   * @param attestatiPassword pass
   */
  public static void fetchData(@Valid Office office, Integer year, Integer month,
                               final String attestatiLogin, final String attestatiPassword, boolean updateSession) {

    rules.checkIfPermitted(office);
    IWrapperOffice wrOffice = factory.create(office);

    // Sessione in cache
    SessionAttestati sessionAttestati = loadAttestatiLoginCached();

    if (updateSession) {
      //cambio mese, effettuo la validazione prima di fetchare.
      if (!validation.hasErrors()) {
        //controllo che il mese sia uploadable
        if (!wrOffice.isYearMonthUploadable(new YearMonth(year, month))) {
          validation.addError("year", "non può essere precedente al primo mese riepilogabile");
          validation.addError("month", "non può essere precedente al primo mese riepilogabile");
        }
      }

      if (validation.hasErrors()) {
        response.status = 400;
        log.warn("validation errors: {}", validation.errorsMap());
        Optional<YearMonth> monthToUpload = Optional.of(new YearMonth(year, month));
        render("@changeMonth", wrOffice, monthToUpload, sessionAttestati);
      }

      //ripulire lo stato della sessione
      sessionAttestati = new SessionAttestati(sessionAttestati.getUsernameCnr(), true,
          sessionAttestati.getCookies(), office, year, month);

      sessionAttestati = attestatiClient.login(URL_TO_PRESENCE,
          null, null, sessionAttestati, office, year, month);

      if (sessionAttestati != null) {
        flash.success("Sessione aggiornata.");
      }
      memAttestatiIntoCache(sessionAttestati, null);
    }

    // Nuovo login e scarico lista dei dipendenti in attestati
    if (attestatiLogin != null && attestatiPassword != null) {
      try {
        memAttestatiIntoCache(null, null);

        if (year == null || month == null) {
          Optional<YearMonth> next = wrOffice.nextYearMonthToUpload();
          Verify.verify(next.isPresent());
          year = next.get().getYear();
          month = next.get().getMonthOfYear();
        }

        sessionAttestati = attestatiClient.login(URL_TO_PRESENCE,
            attestatiLogin, attestatiPassword, null, office, year, month);

        if (!sessionAttestati.isLoggedIn()) {
          flash.error("Errore durante il login sul sistema degli attestati.");
          UploadSituation.uploadData(office.id);
        }

        memAttestatiIntoCache(sessionAttestati, null);

      } catch (AttestatiException e) {
        flash.error(String.format("Errore durante il login e/o prelevamento della lista "
            + "dei dipendenti dal sistema degli attestati. Eccezione: {}", e));
        UploadSituation.uploadData(office.id);
      }
    } else {

      if (sessionAttestati == null || !sessionAttestati.isLoggedIn()) {
        flash.error("La sessione attestati non è attiva o è scaduta, effettuare nuovamente login.");
        UploadSituation.uploadData(office.id);
      }
    }

    uploadData(office.id);
  }

  /**
   * Logout attestati.
   *
   * @param officeId sede
   */
  public static void logoutAttestati(Long officeId) {

    memAttestatiIntoCache(null, null);
    flash.success("Logout attestati eseguito.");
    UploadSituation.uploadData(officeId);
  }


  /**
   * Elabora i dati di tutte le persone della sede per quel mese.
   *
   * @param office sede
   * @param year   anno
   * @param month  mese
   */
  public static void processAllPersons(Office office, int year, int month)
      throws MalformedURLException, URISyntaxException {

    rules.checkIfPermitted(office);

    SessionAttestati sessionAttestati = loadAttestatiLoginCached();

    if (sessionAttestati == null || !sessionAttestati.isLoggedIn()) {
      flash.error("La sessione attestati non è attiva o è scaduta, effettuare nuovamente login.");
      UploadSituation.uploadData(office.id);
    }

    if (sessionAttestati.getOfficesDips().get(office) == null) {
      flash.error("La sede per la quale si vuole caricare gli attestati non è abilitata "
          + "su Attestati CNR, effettuare un nuovo login e riprovare.");
      uploadData(office.id);
    }
    if (sessionAttestati.getYear() != year || sessionAttestati.getMonth() != month) {
      flash.error("Per caricare i dati di un mese diverso da quello corrente "
          + "utilizzare l'apposita funzione di cambio mese.");
      uploadData(office.id);
    }

    DipendenteComparedRecap dipendenteComparedRecap = attestatiClient
        .buildComparedLists(office, sessionAttestati);

    List<RispostaElaboraDati> checks = attestatiClient.elaboraDatiDipendenti(
        Optional.fromNullable(sessionAttestati), dipendenteComparedRecap.getValidDipendenti(),
        year, month);

    Predicate<RispostaElaboraDati> rispostaOk = new Predicate<RispostaElaboraDati>() {
      @Override
      public boolean apply(RispostaElaboraDati risposta) {
        return risposta.getProblems() == null || risposta.getProblems().isEmpty();
      }
    };

    List<RispostaElaboraDati> risposteNotOk =
        FluentIterable.from(checks).filter(Predicates.not(rispostaOk)).toList();

    if (risposteNotOk.isEmpty()) {
      flash.success("Elaborazione dipendenti effettuata senza errori.");
    } else if (risposteNotOk.size() == 1) {
      flash.error("Elaborazione dipendenti effettuata. Sono stati riscontrati problemi per "
          + "1 dipendente. Controllare l'esito.");
    } else {
      flash.error("Elaborazione dipendenti effettuata. Sono stati riscontrati problemi per %s"
              + " dipendenti. Controllare l'esito.",
          risposteNotOk.size());
    }

    UploadSituation.uploadData(office.id);


  }

  /**
   * Elabora i dati della matricola.
   *
   * @param officeId  sede
   * @param matricola matricola
   * @param year      anno
   * @param month     mese
   */
  public static void processSinglePerson(Long officeId, String matricola, int year, int month)
      throws MalformedURLException, URISyntaxException {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    notFoundIfNull(matricola);
    rules.checkIfPermitted(office);
    SessionAttestati sessionAttestati = loadAttestatiLoginCached();

    if (sessionAttestati == null || !sessionAttestati.isLoggedIn()) {
      flash.error("La sessione attestati non è attiva o è scaduta, effettuare nuovamente login.");
      UploadSituation.uploadData(office.id);
    }

    if (sessionAttestati.getOfficesDips().get(office) == null) {
      flash.error("La sede per la quale si vuole caricare gli attestati non è abilitata "
          + "su Attestati CNR, effettuare un nuovo login e riprovare.");
      uploadData(office.id);
    }
    if (sessionAttestati.getYear() != year || sessionAttestati.getMonth() != month) {
      flash.error("Per caricare i dati di un mese diverso da quello corrente "
          + "utilizzare l'apposita funzione di cambio mese.");
      uploadData(office.id);
    }

    DipendenteComparedRecap dipendenteComparedRecap = attestatiClient
        .buildComparedLists(office, sessionAttestati);

    Optional<Dipendente> dipendente = Optional.<Dipendente>absent();
    for (Dipendente dipCached : dipendenteComparedRecap.getValidDipendenti()) {
      if (dipCached.getMatricola().equals(matricola)) {
        dipendente = Optional.fromNullable(dipCached);
        break;
      }
    }

    if (!dipendente.isPresent()) {
      flash.error("Il dipendente selezionato non è presente nell'elenco. Effettuare"
          + "nuovamente login attestati e riprovare, oppure inviare una segnalazione.");
      UploadSituation.uploadData(office.id);
    }

    List<RispostaElaboraDati> checks = attestatiClient
        .elaboraDatiDipendenti(Optional.fromNullable(sessionAttestati),
            Lists.newArrayList(dipendente.get()), year, month);

    Predicate<RispostaElaboraDati> rispostaOk = new Predicate<RispostaElaboraDati>() {
      @Override
      public boolean apply(RispostaElaboraDati risposta) {
        return risposta.getProblems() == null || risposta.getProblems().isEmpty();
      }
    };

    List<RispostaElaboraDati> risposteNotOk =
        FluentIterable.from(checks).filter(Predicates.not(rispostaOk)).toList();

    if (risposteNotOk.isEmpty()) {
      flash.success("Elaborazione dipendente effettuata senza errori.");
    } else {
      flash.error("Elaborazione dipendente effettuata. Sono stati riscontrati problemi per "
          + "1 dipendente. Controllare l'esito.");
    }

    UploadSituation.uploadData(office.id);

  }

  /**
   * Modale visualizzazione problemi.
   *
   * @param certificatedDataId dati certificati
   */
  public static void showProblems(Long certificatedDataId) {
    rules.checkIfPermitted(Security.getUser().get().person.office);
    CertificatedData cd = personMonthRecapDao.getCertificatedDataById(certificatedDataId);
    if (cd == null) {
      renderText("L'elaborazione attestati richiesta è inesistente.");
    }
    render(cd);
  }

  /**
   * Modale visualizzazione dati inviati.
   *
   * @param certificatedDataId dati certificati
   */
  public static void showCertificatedData(Long certificatedDataId) {
    rules.checkIfPermitted(Security.getUser().get().person.office);
    CertificatedData cd = personMonthRecapDao.getCertificatedDataById(certificatedDataId);
    if (cd == null) {
      renderText("L'elaborazione attestati richiesta è inesistente.");
    }
    render(cd);
  }

  /**
   * Verifica i dati che ePAS invierebbe ad Attestati. Visualizza anche gli eventuali dati mandati e
   * gli errori riportati.
   *
   * @param officeId sede
   */
  public static void checkData(Long officeId, Integer year, Integer month) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);

    IWrapperOffice wrOffice = factory.create(office);
    render(wrOffice, year, month);

  }

  /**
   * Visualizza i dati certificati per la sede in quel mese.
   *
   * @param office sede
   * @param year   anno
   * @param month  mese
   */
  public static void performCheckData(Office office, Integer year, Integer month)
      throws MalformedURLException, URISyntaxException {

    notFoundIfNull(office);
    rules.checkIfPermitted(office);

    IWrapperOffice wrOffice = factory.create(office);
    Set<Dipendente> people = attestatiClient.officeActivePeopleAsDipendente(office, year, month);

    List<RispostaElaboraDati> results = attestatiClient.elaboraDatiDipendenti(
        Optional.<SessionAttestati>absent(), Lists.newArrayList(people), year, month);

    IWrapperFactory wrapper = factory;
    render("@checkData", results, wrOffice, year, month, wrapper);

  }

  private static void memAttestatiIntoCache(
      SessionAttestati sessionAttestati, List<Dipendente> listaDipendenti) {

    Cache.set(LOGIN_RESPONSE_CACHED + Security.getUser().get().username, sessionAttestati);
    Cache.set(LISTA_DIPENTENTI_CNR_CACHED + Security.getUser().get().username, listaDipendenti);
  }

  /**
   * Carica in cache lo stato della connessione con attestati.cnr
   */
  private static SessionAttestati loadAttestatiLoginCached() {
    return (SessionAttestati) Cache.get(LOGIN_RESPONSE_CACHED + Security.getUser().get().username);
  }


}
