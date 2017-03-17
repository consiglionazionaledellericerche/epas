package manager.attestati.old;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import controllers.UploadSituation;

import dao.AbsenceDao;
import dao.CompetenceDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.PersonMonthRecapDao;
import dao.wrapper.IWrapperFactory;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import manager.PersonDayManager;

import models.CertificatedData;
import models.Competence;
import models.Office;
import models.Person;
import models.PersonMonthRecap;
import models.absences.Absence;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import play.Logger;

/**
 * Incapsula le funzionalità necessarie per l'interazione via HTTP GET/POST con il sistema degli
 * attestati del CNR di Roma.
 *
 * @author cristian
 */
@Slf4j
public class AttestatiClient {

  private static String CLIENT_USER_AGENT = "ePAS";
  private static String BASE_LOGIN_URL = "LoginLDAP";
  private static String BASE_LISTA_DIP_MASK_URL = "ListaDipMask";
  private static String BASE_LISTA_DIPENDENTI_URL = "ListaDip";
  private static String BASE_ELABORA_DATI_URL = "HostDip";
  
  @Inject
  private PersonDao personDao;
  @Inject 
  private OfficeDao officeDao;
  @Inject
  private PersonMonthRecapDao personMonthRecapDao;
  @Inject
  private AbsenceDao absenceDao;
  @Inject
  private PersonDayDao personDayDao;
  @Inject
  private PersonDayManager personDayManager;
  @Inject
  private CompetenceDao competenceDao;
  @Inject
  private IWrapperFactory factory;

  /**
   * La lista di assenze passate sono di tipo giornaliero ma al sistema degli attestati vanno
   * passate le assenze come intervallo di tempo. Questo metodo accorpa le assenze dello stesso tipo
   * effettuate in giorni conseguitivi in un'unica assenza di tipo AssenzaPerPost e ritorna la lista
   * delle assenze accorpate.
   *
   * @param absences la lista delle assenze giornaliere da accorpare in oggetti AssenzaPerPost
   * @return la lista delle assenze accorpate in oggetti di tipo AssenzaPerPost
   */
  private static List<AssenzaPerPost> getAssenzePerPost(List<Absence> absences) {
    List<AssenzaPerPost> assenze = Lists.newLinkedList();

    LocalDate previousDate = null;
    String previousAbsenceCode = null;
    AssenzaPerPost assenza = null;

    for (Absence absence : absences) {
      
      //codici a uso interno li salto
      if (absence.absenceType.internalUse) {
        continue;
      }
      //codice per attestati
      String absenceCodeToSend = absence.absenceType.code.toUpperCase();
      if (absence.absenceType.certificateCode != null 
          && !absence.absenceType.certificateCode.trim().isEmpty()) { 
        absenceCodeToSend = absence.absenceType.certificateCode.toUpperCase();
      }

      if (previousDate == null || previousAbsenceCode == null) {
        assenza = new AssenzaPerPost(absenceCodeToSend, absence.personDay.date.getDayOfMonth());
        assenze.add(assenza);
        previousDate = absence.personDay.date;
        previousAbsenceCode = absenceCodeToSend;
        continue;
      }

      if (previousDate.plusDays(1).equals(absence.personDay.date)
          && previousAbsenceCode.equals(absenceCodeToSend)) {
        assenza.setGgFine(absence.personDay.date.getDayOfMonth());
      } else {
        assenza = new AssenzaPerPost(absenceCodeToSend, absence.personDay.date.getDayOfMonth());
        previousAbsenceCode = absenceCodeToSend;
        assenze.add(assenza);
      }
      previousDate = absence.personDay.date;

    }

    return assenze;
  }

  /**
   * Effettua login sul sito degli attestati. Se già presente una sessione la annulla e ricarica
   * tutte le informazioni sul mese selezionato. 
   * Preleva la lista degli office abilitati all'invio delle presenze e tutti i dipendenti 
   * che risultano su Attestati per l'anno/mese selezionato.
   * 
   * @param urlToPresence url per attestato.
   * @param attestatiLogin username
   * @param attestatiPassword password
   * @param year anno 
   * @param month mese
   */
  public SessionAttestati login(String urlToPresence, String attestatiLogin, 
      String attestatiPassword, SessionAttestati sessionAttestati, Office office, 
      Integer year, Integer month) {

    try {
      final URI baseUri = new URI(urlToPresence);

      // 1) Login
      if (sessionAttestati == null || !sessionAttestati.loggedIn) {
        final URL loginUrl = baseUri.resolve(BASE_LOGIN_URL).toURL();
        Connection connection = Jsoup.connect(loginUrl.toString());
        Response loginResponse = connection
            .data("utente", attestatiLogin)
            .data("login", attestatiPassword)
            //.data("utente", "claudio.baesso")
            //.data("login", "a")
            .userAgent(CLIENT_USER_AGENT)
            .url(loginUrl)
            .method(Method.POST).execute();
        
        log.info("Effettuata la richiesta di login all'indirizzo {} come utente {}, "
            + "codice di risposta http = {} e con messaggio {}.",
            loginUrl, attestatiLogin, loginResponse.statusCode(), loginResponse.statusMessage());

        Document loginDoc = loginResponse.parse();
        log.debug("Risposta alla login = \n{}", loginDoc);
        
        Elements loginMessages = loginDoc.select("h5[align=center]>font");
        //log.info("Login messages: {}", loginMessages.first().ownText());
        if (loginMessages.isEmpty()
            || !loginMessages.first().ownText().contains("Login completata con successo.")) {
          //errore login
          return new SessionAttestati(
              attestatiLogin, false, loginResponse.cookies(), office, year, month);
        }

        sessionAttestati = new SessionAttestati(attestatiLogin,
            true, loginResponse.cookies(), office, year, month);
      }
      // 2) Sedi abilitate
      final URL listaDipendentiMaskUrl = baseUri.resolve(BASE_LISTA_DIP_MASK_URL).toURL();
      Connection connection = Jsoup.connect(listaDipendentiMaskUrl.toString());
      connection.cookies(sessionAttestati.getCookies());

      Response listaDipendentiMaskResponse = connection
          .userAgent(CLIENT_USER_AGENT)
          .method(Method.GET).execute();

      log.info("Effettuata la richiesta di sedi disponibili {}, codice di risposta http = {}",
          attestatiLogin, listaDipendentiMaskResponse.statusCode());

      if (listaDipendentiMaskResponse.statusCode() != 200) {
        throw new AttestatiException(
            String.format("Impossibile prelevare la lista delle sedi abilitate."
                + "Il sistema remote ha restituito il codice di errore http = %d."
                + "Contattare l'amministratore di ePAS per maggiori informazioni.",
                listaDipendentiMaskResponse.statusCode()));
      }

      Document listaDipendentiMaskDoc = listaDipendentiMaskResponse.parse();
      //  <TD WIDTH='24%'>
      //    <SELECT NAME='sede_id'>
      //      <OPTION>223400</OPTION>
      //      <OPTION>223400</OPTION>
      //      <OPTION>223410</OPTION>
      //    </SELECT>
      //  </TD>
      Elements selectSeat = listaDipendentiMaskDoc.select("select[name*=sede_id]");
      for (Element option : selectSeat.first().children()) {
        try {
          Optional<Office> officeCnr = officeDao.byCodeId(option.text());
          if (officeCnr.isPresent()) {
            if (sessionAttestati.getOfficesDips().get(officeCnr.get()) == null) {
              // caricare le persone.
              Set<Dipendente> officeDips = listaDipendenti(officeCnr.get(), 
                  sessionAttestati.getCookies(), year, month);
              sessionAttestati.getOfficesDips().put(officeCnr.get(), officeDips);
              log.debug("Ho prelevato la sede {} con {} dipendenti.", officeCnr.get(),
                  officeDips.size());
            }
          }
        } catch (Exception ex) {
          log.error("Eccezione durante il prelevamento dei dati da Attestati.", ex);
        }
      }
      
      //Genero la lista degli anni per le sedi individuate ...
      Set<Integer> yearsSet = Sets.newHashSet();
      for (Office officeCnr : sessionAttestati.getOfficesDips().keySet()) {
        yearsSet.addAll(factory.create(officeCnr).getYearUploadable());
      }
      sessionAttestati.setYearsList(Lists.newArrayList(yearsSet));
      Collections.sort(sessionAttestati.getYearsList());
      
      //Imposto l'office corrente
      if (sessionAttestati.officesDips.keySet().contains(office)) {
        sessionAttestati.setOffice(office);
      } else {
        log.debug("La sede %s non è presente in quelle permesse %s.", office,
            sessionAttestati.getOfficesDips().keySet());
        return new SessionAttestati(attestatiLogin, false, null, office, year, month);
      }

      return sessionAttestati;

    } catch (IOException ex) {
      log.error("Errore durante la login e fetch informazioni sistema di invio degli attestati."
          + " Eccezione = {}", ex);
    } catch (URISyntaxException e1) {
      log.error("Errore durante la login e fetch informazioni sistema di invio degli attestati."
          + " Eccezione = {}", e1);      
    }

    return new SessionAttestati(attestatiLogin, false, null, office, year, month);
  }

  /**
   * Estrae la lista dei dipendenti dall'HTML di attestati.
   *
   * @param cookies i cookies da utilizzare per inviare una richiesta "autenticata"
   * @return la lista dei dipendenti estratta dall'HTML della apposita pagina prevista nel sistema
   *     degli attestati di Roma
   */
  public Set<Dipendente> listaDipendenti(Office office,
      Map<String, String> cookies, Integer year, Integer month)
      throws URISyntaxException, MalformedURLException {
    Response listaDipendentiResponse;

    String urlToPresence = UploadSituation.URL_TO_PRESENCE;

    URI baseUri = new URI(urlToPresence);
    final URL listaDipendentiUrl = baseUri.resolve(BASE_LISTA_DIPENDENTI_URL).toURL();
    Connection connection = Jsoup.connect(listaDipendentiUrl.toString());
    connection.cookies(cookies);

    try {
      listaDipendentiResponse = connection
              .data("sede_id", office.codeId)
              .data("anno", year.toString())
              .data("mese", month.toString())
              .userAgent(CLIENT_USER_AGENT)
              //.url(listaDipendentiUrl)
              .method(Method.POST).execute();

      log.debug(
          "Effettuata la richiesta per avere la lista dei dipendenti, codice di risposta http = {}",
          listaDipendentiResponse.statusCode());

      if (listaDipendentiResponse.statusCode() != 200) {
        throw new AttestatiException(
                String.format("Impossibile prelevare la lista dei dipendenti da %s. "
                                + "Il sistema remote ha restituito il codice di errore http = %d."
                                + "Contattare l'amministratore di ePAS per maggiori informazioni.",
                        listaDipendentiUrl, listaDipendentiResponse.statusCode()));
      }

      Document listaDipendentiDoc = listaDipendentiResponse.parse();

      log.debug("Risposta alla richiesta della lista dei dipendenti = \n {}", listaDipendentiDoc);

      /*
       * Snippet di codice html da parsare per avere le matricole e il nome del dipendente:
       *
       * <tr>
       *  <td align="right"> <font size="2" color="#0000FF" face="Arial"> <b>1</b> </font> </td>
       *  <td align="right"> <font size="3" color="#0000FF" face="Arial">
       *    <b><a href="DettDip?matr=14669&amp;anno=2013&amp;mese=10&amp;sede_id=223400&amp;ddpage=parziale">14669</a> </b> </font>
       *  </td>
       *  <td align="left">
       *      <font size="1" color="#0000FF" face="Arial">VIVALDI ANDREA &nbsp; </font></td>
       *  <td align="middle"> <font size="1" color="#0000FF" face="Arial">1/2/2012</font></td>
       *  <td align="middle"> <font size="1" color="#0000FF" face="Arial">31/1/2014</font></td>
       *  <td align="middle"> <font size="1" color="#0000FF" face="Arial">NO</font></td>
       * </tr>
       */
      Set<Dipendente> listaDipendenti = Sets.newHashSet();
      Elements anchorMatricole = listaDipendentiDoc.select("a[href*=DettDip?matr=]");
      for (Element e : anchorMatricole) {
        String matricola = e.ownText();
        Element tdMatricola = e.parent().parent().parent();
        //The HTML entity &nbsp; (Unicode character NO-BREAK SPACE U+00A0) can in
        //Java be represented by the character \u00a0
        String nomeCognome =
            tdMatricola.siblingElements().get(1).text().replace("\u00a0", "").trim();
        log.debug("Nel html della lista delle persone individuato \"{}\", matricola=\"{}\"",
            nomeCognome, matricola);
        Person person = personDao.getPersonByNumber(Integer.parseInt(matricola));
        listaDipendenti.add(new Dipendente(person, nomeCognome));
      }

      return listaDipendenti;

    } catch (IOException ex) {
      log.error("Errore durante il prelevamento della lista dei dipendneti. Eccezione = {}",
          ex.getStackTrace().toString());
      throw new AttestatiException(
          String.format("Errore durante il prelevamento della lista dei dipendneti. Eccezione = %s",
              ex.getStackTrace().toString()));
    }
  }

  /**
   * Invia la richiesta di elaborazione di un dipendente. 
   */
  private RispostaElaboraDati elaboraDatiDipendenteClient(
          Map<String, String> cookies, Dipendente dipendente, Integer year, Integer month,
          List<Absence> absences, List<Competence> competences,
          List<PersonMonthRecap> pmList, Integer mealTicket, boolean performSent)
          throws URISyntaxException, MalformedURLException {

    //Office office = Security.getUser().get().person.office;
    Office office = dipendente.getPerson().office;
    String urlToPresence = UploadSituation.URL_TO_PRESENCE;
    URI baseUri = new URI(urlToPresence);
    final URL elaboraDatiUrl = baseUri.resolve(BASE_ELABORA_DATI_URL).toURL();

    //Connessione
    Connection connection = Jsoup.connect(elaboraDatiUrl.toString());
    if (performSent) {
      connection.cookies(cookies);
      connection.userAgent(CLIENT_USER_AGENT)
        .data("matr", dipendente.getMatricola())
        .data("anno", year.toString())
        .data("mese", month.toString())
        .data("sede_id", office.codeId)
        .method(Method.POST);
    }

    // Invio le Assenze
    int codAssAssoCounter = 0;
    StringBuffer absencesSent = new StringBuffer();
    for (AssenzaPerPost assenzaPerPost : getAssenzePerPost(absences)) {
      absencesSent.append(assenzaPerPost.getCodice()).append(",")
      .append(assenzaPerPost.getGgInizio()).append(",")
      .append(assenzaPerPost.getGgFine()).append("; ");
      if (performSent) {
        connection.data("codass" + codAssAssoCounter, assenzaPerPost.getCodice());
        connection.data("gg_inizio" + codAssAssoCounter, assenzaPerPost.getGgInizio().toString());
        connection.data("gg_fine" + codAssAssoCounter, assenzaPerPost.getGgFine().toString());
        log.info("{}, sto spedendo l'assenza di tipo {}, gg inizio = {}, gg_fine = {}",
            dipendente.getCognomeNome(), assenzaPerPost.getCodice(),
            assenzaPerPost.getGgInizio(), assenzaPerPost.getGgInizio());
      }
      codAssAssoCounter++;
    }

    // Invio le Competenze
    int codComCounter = 0;
    StringBuffer competencesSent = new StringBuffer();
    for (Competence competence : competences) {
      competencesSent.append(competence.competenceCode.code).append(",")
      .append(competence.valueApproved).append("; ");
      if (performSent) {
        connection.data("codcom" + codComCounter, competence.competenceCode.code);
        connection.data("oreatt" + codComCounter, String.valueOf(competence.valueApproved));
        log.info("{}, sto spedendo la competenza di tipo {}, ore attribuite = {}",
            dipendente.getCognomeNome(), competence.competenceCode.code, competence.valueApproved);
      }
      codComCounter++;
    }

    // Invio le ore di formazione
    int counter = 0;
    StringBuffer trainingHoursSent = new StringBuffer();
    if (pmList != null) {
      for (PersonMonthRecap pm : pmList) {
        trainingHoursSent.append(String.valueOf(pm.fromDate.getDayOfMonth())).append(",")
        .append(String.valueOf(pm.toDate.getDayOfMonth())).append(",")
                .append(String.valueOf(pm.trainingHours)).append("; ");
        if (performSent) {
          connection.data("gg_inizio_corso" + counter, String.valueOf(pm.fromDate.getDayOfMonth()));
          connection.data("gg_fine_corso" + counter, String.valueOf(pm.toDate.getDayOfMonth()));
          connection.data("ore_corso" + counter, String.valueOf(pm.trainingHours));
          log.info("{}, sto spedendo {} ore di formazione dal giorno {} al giorno {}",
              dipendente.getCognomeNome(), pm.trainingHours,
              pm.fromDate, pm.toDate);
        }
        counter++;
      }
    }

    // Invio i buoni pasto
    StringBuffer mealTicketSent = new StringBuffer();
    if (mealTicket != null) {
      mealTicketSent.append(String.valueOf(year)).append(",")
      .append(String.valueOf(month)).append(",").append(String.valueOf(mealTicket));
      if (performSent) {
        connection.data("gg_buoni_pasto", String.valueOf(mealTicket));
        log.info("Inviati {} buoni pasto per {}", mealTicket, dipendente.getCognomeNome());
      }
    }

    // Invio i dati
    Response elaboraDatiResponse;
    boolean isResponseOk = true;
    StringBuffer problems = new StringBuffer();
    if (performSent) {
      try {
        elaboraDatiResponse = connection.execute();

        log.debug("Effettuata l'elaborazione dati del dipendente {} (matricola {}) per l'anno {}, "
            + "mese {}. Codice di risposta http = {}",
            dipendente.getCognomeNome(), dipendente.getMatricola(), year, month,
            elaboraDatiResponse.statusCode());

        if (elaboraDatiResponse.statusCode() != 200) {
          throw new AttestatiException(
              String.format("Errore durante l'elaborazione dati del dipendente %s",
                  dipendente.getCognomeNome()));
        }

        Document elaboraDatiDoc = elaboraDatiResponse.parse();
        Logger.info("Risposta all'elaborazione dati = \n%s", elaboraDatiDoc);

        /*
         * In caso di errore nella pagina restituita compaiono degli H5 come questi:
         *    <H5 align=center><FONT SIZE='4' FACE='Arial'>
         *      Errore in fase di controllo competenze <BR>
         *      7  ERRASSSOVRAPP<BR>Assenza  OA7 in periodi sovrapposti </FONT>
         *    </H5>
         *    <BR>Controllo Competenze --> ..Effettuato!
         *    <B>Non sono state inserite competenze</B>
         *    <H5 align=center><FONT SIZE='4' FACE='Arial'>
         *      Errore in fase di controllo assenze dipendente=9535, mese=10, anno=2013, errore=7
         *      ERRASSSOVRAPP<BR>Assenza  OA7 in periodi sovrapposti </FONT>
         *    </H5>
         */

        Elements errorElements = elaboraDatiDoc.select("h5[align=center]>font");
        if (errorElements.isEmpty()) {
          /*TODO: controllare anche che ci sia scritto:
           *
           * <BR>Controllo Competenze --> ..Effettuato!
           * <B>Non sono state inserite competenze</B>
           * <BR>Controllo Assenze --> ..Effettuato!
           * <BR>Aggiornamento Competenze --> ..Effettuato! <B></B>
           * <BR>Aggiornamento Assenze --> ..Effettuato! <B></B><BR>
           */
        } else {
          //Si aggiunge il contenuto testuale degli elementi font che contengono il
          //messaggio di errore
          for (Element el : errorElements) {
            problems.append(el.ownText()).append(" | ");
          }
          isResponseOk = false;
        }
      } catch (IOException e) {
        log.error("Errore la chiamata alla funzione \"elabora dati\" sistema di invio degli "
            + "attestati. Eccezione = {}", e.getStackTrace().toString());
        throw new AttestatiException(
            String.format("Impossibile effettuare l'elaborazione dati su %s", elaboraDatiUrl));
      }
    } else {
      //Oppure niente ... senza problemi e con risposta automaticamente ok.
    }

    RispostaElaboraDati resp =
        new RispostaElaboraDati(dipendente.getCognomeNome(), dipendente.getMatricola());
    resp.setAbsencesSent(absencesSent.length() > 0 ? absencesSent.toString() : null);
    resp.setProblems(problems.length() > 0 ? problems.toString() : null);
    resp.setCompetencesSent(competencesSent.length() > 0 ? competencesSent.toString() : null);
    resp.setTrainingHoursSent(trainingHoursSent.length() > 0 ? trainingHoursSent.toString() : null);
    resp.setMealTicketSent(mealTicketSent.length() > 0 ? mealTicketSent.toString() : null);
    resp.setOk(isResponseOk);
    resp.setDipendente(dipendente);
    return resp;
  }
  
  /**
   * Elabora i dati dei dipendenti presenti nella lista. Se la sessione è presente invia i dati
   * ad attestati.
   * 
   */
  public List<RispostaElaboraDati> elaboraDatiDipendenti(
      Optional<SessionAttestati> sessionAttestati, 
      List<Dipendente> dipendenti, Integer year, Integer month)
          throws MalformedURLException, URISyntaxException {
    
    List<RispostaElaboraDati> checks = Lists.newLinkedList();

    /**
     * In questo punto devo mettere le chiamate rest per ciascuno dei parametri da inviare ad 
     * attestati: 
     * invece di inviare persona per persona tutte le informazioni, occorre inviare per ciascuna 
     * informazione (assenze, competenze, ore di formazione, buoni pasto) la lista dei dipendenti
     * con le rispettive info come da documento inviato da pagano 
     * 
     */
    for (Dipendente dipendente : dipendenti) {

      if (dipendente.getMatricola() == null || dipendente.getMatricola().isEmpty()) {
        continue;
      }
      Person person = personDao.getPersonByNumber(Integer.parseInt(dipendente.getMatricola()));

      
      //Ore formazione
      List<PersonMonthRecap> trainingHoursList = personMonthRecapDao
          .getPersonMonthRecapInYearOrWithMoreDetails(person, year, 
              Optional.fromNullable(month), Optional.<Boolean>absent());

      //Buoni Pasto
      Integer mealTicket = personDayManager.numberOfMealTicketToUse(personDayDao
          .getPersonDayInMonth(person, new YearMonth(year, month)));
      
      //Assenze
      List<Absence> absences = absenceDao.getAbsencesNotInternalUseInMonth(person, year, month);
      
      //Competenze
      List<Competence> competences = competenceDao
          .getCompetenceInMonthForUploadSituation(person, year, month);
      
      //Dati inviati
      CertificatedData cert = personMonthRecapDao.getPersonCertificatedData(person, month, year);
      
      //Se la sessione è presente faccio perform.
      if (sessionAttestati.isPresent()) {
        RispostaElaboraDati rispostaElaboraDati = elaboraDatiDipendenteClient(
            sessionAttestati.get().getCookies(), dipendente, year, month,
            absences, competences, trainingHoursList, mealTicket, true);
        if (rispostaElaboraDati.isOk()) {
          for (PersonMonthRecap personMonth : trainingHoursList) {
            personMonth.hoursApproved = true;
            personMonth.save();
          }
        }
        if (cert == null) {
          //FIXME
          //queste variabili di appoggio sono state inserite perchè richiamandole direttamente nel
          //costruttore veniva lanciata l'eccezione
          //play.exceptions.JavaExecutionException:
          //  models.CertificatedData.<init>(Lmodels/Person;Ljava/lang/String;Ljava/lang/String;II)V
          int anno = year;
          int mese = month;
          String cognomeNome = dipendente.getCognomeNome();
          String matricola = dipendente.getMatricola();
          cert = new CertificatedData(person, cognomeNome, matricola, anno, mese);
        }
        cert.absencesSent = rispostaElaboraDati.getAbsencesSent();
        cert.competencesSent = rispostaElaboraDati.getCompetencesSent();
        cert.mealTicketSent = rispostaElaboraDati.getMealTicketSent();
        cert.trainingHoursSent = rispostaElaboraDati.getTrainingHoursSent();
        cert.problems = rispostaElaboraDati.getProblems();
        cert.isOk = rispostaElaboraDati.isOk();
        cert.save();
        checks.add(rispostaElaboraDati);
      } else {
        //e' una previsione.
        RispostaElaboraDati rispostaElaboraDati = elaboraDatiDipendenteClient(
            null, dipendente, year, month,
            absences, competences, trainingHoursList, mealTicket, false);
        checks.add(rispostaElaboraDati);
      }
    }

    return checks;
  }

  /**
   * Contenitore dei dati necessari per l'invio dei periodi di assenza del personale tramite il
   * sistema degli attestati del CNR.
   *
   * @author cristian
   */
  private static final class AssenzaPerPost {
    private final String codice;
    private final Integer ggInizio;
    private Integer ggFine;

    public AssenzaPerPost(String codice, Integer ggInizio) {
      this.codice = codice;
      this.ggInizio = ggInizio;
      this.ggFine = ggInizio;
    }

    public String getCodice() {
      return codice;
    }

    public Integer getGgInizio() {
      return ggInizio;
    }

    public Integer getGgFine() {
      return ggFine;
    }

    public void setGgFine(Integer ggFine) {
      this.ggFine = ggFine;
    }

  }
  
  /**
   * Costruisce l'oggetto che contiene le liste comparate dei dipendenti.
   * (Quelli attivi su CNR, quelli attivi su EPAS e le differenze).
   */
  public DipendenteComparedRecap buildComparedLists(Office office, 
      SessionAttestati sessionAttestati) {
    
    DipendenteComparedRecap recap = new DipendenteComparedRecap();
    recap.cnrDipendenti = sessionAttestati.getOfficesDips().get(office);
    
    int year = sessionAttestati.getYear();
    int month = sessionAttestati.getMonth();

    recap.epasDipendenti = officeActivePeopleAsDipendente(office, year, month);
    
    recap.validDipendenti = Lists.newArrayList(
        Sets.intersection(recap.cnrDipendenti, recap.epasDipendenti));
    Collections.sort(recap.validDipendenti);
    
    recap.notInEpasDipendenti = getDipendenteNonInEpas(recap.cnrDipendenti, recap.epasDipendenti);
    recap.notInCnrDipendenti = getDipendenteNonInCnr(recap.cnrDipendenti, recap.epasDipendenti);
    
    return recap;
  }
    
  private Set<Dipendente> getDipendenteNonInEpas(Set<Dipendente> listaDipendenti,
      Set<Dipendente> activeDipendenti) {
   
    Set<Dipendente> dipendentiNonInEpas =
        Sets.difference(ImmutableSet.copyOf(listaDipendenti), activeDipendenti);
    if (dipendentiNonInEpas.size() > 0) {
      log.info("I seguenti dipendenti sono nell'anagrafica CNR ma non in ePAS. {}",
          dipendentiNonInEpas);
    }
    return dipendentiNonInEpas;
  }

  private Set<Dipendente> getDipendenteNonInCnr(Set<Dipendente> listaDipendenti,
      Set<Dipendente> activeDipendenti) {
    Set<Dipendente> dipendentiNonInCnr =
        Sets.difference(activeDipendenti, ImmutableSet.copyOf(listaDipendenti));
    if (dipendentiNonInCnr.size() > 0) {
      log.info("I seguenti dipendenti sono nell'anagrafica di ePAS ma non in quella del CNR. {}",
          dipendentiNonInCnr);
    }
    return dipendentiNonInCnr;
  }

  /**
   * Costruisce l'insieme di Dipendenti con le persone attive nel mese in ePAS.
   * @param office sede
   * @param year anno 
   * @param month mese
   */
  public Set<Dipendente> officeActivePeopleAsDipendente(Office office, int year, int month) {

    List<Person> persons = personDao.list(Optional.<String>absent(),
        Sets.newHashSet(office), false, new LocalDate(year, month, 1),
        new LocalDate(year, month, 1).dayOfMonth().withMaximumValue(), true).list();

    Set<Dipendente> dipendenti = FluentIterable.from(persons)
        .transform(new Function<Person, Dipendente>() {
          @Override
          public Dipendente apply(Person person) {
            Dipendente dipendente =
                new Dipendente(person, Joiner.on(" ").skipNulls().join(person.surname, 
                    person.othersSurnames, person.name));
            return dipendente;
          }
        }).toSet();

    return dipendenti;
  }
  

  @SuppressWarnings("serial")
  @Data
  public static final class SessionAttestati implements Serializable {
    
    private final boolean loggedIn;
    private final Map<String, String> cookies;
    private String usernameCnr;
    private Integer year;
    private Integer month;
    private Office office;
    private Map<Office, Set<Dipendente>> officesDips = Maps.newHashMap();
    private List<Integer> yearsList = Lists.newArrayList();

    public SessionAttestati(String usernameCnr, boolean loggedIn,
        Map<String, String> cookies, Office office, Integer year, Integer month) {
      this.usernameCnr = usernameCnr;
      this.loggedIn = loggedIn;
      this.cookies = cookies;
      this.office = office;
      this.year = year;
      this.month = month;
    }
  }
  
  @Data
  public static final class DipendenteComparedRecap {
    
    Set<Dipendente> cnrDipendenti;
    Set<Dipendente> epasDipendenti;

    List<Dipendente> validDipendenti;
    Set<Dipendente> notInEpasDipendenti;
    Set<Dipendente> notInCnrDipendenti;
  }

 
}
