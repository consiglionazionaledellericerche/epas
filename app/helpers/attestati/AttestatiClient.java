/**
 *
 */
package helpers.attestati;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.common.collect.Lists;

import controllers.Security;
import dao.PersonDao;
import it.cnr.iit.epas.DateUtility;
import manager.ConfGeneralManager;
import models.Absence;
import models.Competence;
import models.Office;
import models.Person;
import models.PersonMonthRecap;
import models.enumerate.Parameter;
import play.Logger;

/**
 * Incapsula le funzionalità necessarie per l'interazione via HTTP GET/POST con il sistema degli
 * attestati del CNR di Roma.
 *
 * @author cristian
 */
public class AttestatiClient {

  private static String CLIENT_USER_AGENT = "ePAS";
  private static String BASE_LOGIN_URL = "LoginLDAP";
  private static String BASE_LISTA_DIPENDENTI_URL = "ListaDip";
  private static String BASE_ELABORA_DATI_URL = "HostDip";
  @Inject
  private ConfGeneralManager confGeneralManager;
  @Inject
  private PersonDao persondao;

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
      String absenceCodeToSend =
              (absence.absenceType.certificateCode == null || absence.absenceType.certificateCode == "")
                      ? absence.absenceType.code.toUpperCase() : absence.absenceType.certificateCode.toUpperCase();

      if (previousDate == null || previousAbsenceCode == null) {
        assenza = new AssenzaPerPost(absenceCodeToSend, absence.personDay.date.getDayOfMonth());
        assenze.add(assenza);
        previousDate = absence.personDay.date;
        previousAbsenceCode = absenceCodeToSend;
        continue;
      }

      if (previousDate.plusDays(1).equals(absence.personDay.date) && previousAbsenceCode.equals(absenceCodeToSend)) {
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
   * Effettua la login sul sistema degli attestati facendo una post con i parametri passati.
   *
   * @param attestatiLogin    nome utente epas
   * @param attestatiPassword password
   * @return la LoginResponse contiene l'ok se la login è andata a buon fine ed in questo caso anche
   * i cookies necessari per le richieste successive.
   */
  public LoginResponse login(String attestatiLogin, String attestatiPassword, Integer year, Integer month) throws AttestatiException, MalformedURLException, URISyntaxException {

    //URI baseUri = new URI(Configuration.getCurrentConfiguration().urlToPresence);
    //ConfGeneral confGeneral =  ConfGeneral.getConfGeneral();
    Office office = Security.getUser().get().person.office;
    String urlToPresence = confGeneralManager.getFieldValue(Parameter.URL_TO_PRESENCE, office);
    URI baseUri = new URI(urlToPresence);
    URL loginUrl = baseUri.resolve(BASE_LOGIN_URL).toURL();

    Connection connection = Jsoup.connect(loginUrl.toString());
    Response loginResponse;
    try {
      loginResponse = connection
              .data("utente", attestatiLogin)
              .data("login", attestatiPassword)
              .userAgent(CLIENT_USER_AGENT)
              .url(loginUrl)
              .method(Method.POST).execute();

      Logger.debug("Effettuata la richiesta di login come utente %s, codice di risposta http = %s",
              attestatiLogin, loginResponse.statusCode());

      Document loginDoc = loginResponse.parse();
      Logger.debug("Risposta alla login = \n%s", loginDoc);


      Elements loginMessages = loginDoc.select("h5[align=center]>font");

      if (loginResponse.statusCode() != 200 ||
              loginMessages.isEmpty() ||
              !loginMessages.first().ownText().contains("Login completata con successo.")) {
        return new LoginResponse(attestatiLogin, false, loginResponse.cookies(), year, month);
      } else {
        return new LoginResponse(attestatiLogin, true, loginResponse.cookies(), year, month);
      }

    } catch (IOException e) {
      Logger.error("Errore durante la login sul sistema di invio degli attestati. Eccezione = %s", e);
      return new LoginResponse(attestatiLogin, false, null, year, month, e);
    }
  }

  /**
   * @param cookies i cookies da utilizzare per inviare una richiesta "autenticata"
   * @return la lista dei dipendenti estratta dall'HTML della apposita pagina prevista nel sistema
   * degli attestati di Roma
   */
  public List<Dipendente> listaDipendenti(Map<String, String> cookies, Integer year, Integer month) throws URISyntaxException, MalformedURLException {
    Response listaDipendentiResponse;

    Office office = Security.getUser().get().person.office;
    String urlToPresence = confGeneralManager.getFieldValue(Parameter.URL_TO_PRESENCE, office);

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

      Logger.debug("Effettuata la richiesta per avere la lista dei dipendenti, codice di risposta http = %d",
              listaDipendentiResponse.statusCode());

      if (listaDipendentiResponse.statusCode() != 200) {
        throw new AttestatiException(
                String.format("Impossibile prelevare la lista dei dipendenti da %s. "
                                + "Il sistema remote ha restituito il codice di errore http = %d."
                                + "Contattare l'amministratore di ePAS per maggiori informazioni.",
                        listaDipendentiUrl, listaDipendentiResponse.statusCode()));
      }

      Document listaDipendentiDoc = listaDipendentiResponse.parse();

      Logger.debug("Risposta alla richiesta della lista dei dipendenti = \n%s", listaDipendentiDoc);

			/*
             * Snippet di codice html da parsare per avere le matricole e il nome del dipendente:
			 *
			 * <tr>
			 *  <td align="right"> <font size="2" color="#0000FF" face="Arial"> <b>1</b> </font> </td>
			 *  <td align="right"> <font size="3" color="#0000FF" face="Arial">
			 *    <b><a href="DettDip?matr=14669&amp;anno=2013&amp;mese=10&amp;sede_id=223400&amp;ddpage=parziale">14669</a> </b> </font>
			 *  </td>
			 *  <td align="left"> <font size="1" color="#0000FF" face="Arial">VIVALDI ANDREA &nbsp; </font></td>
			 *  <td align="middle"> <font size="1" color="#0000FF" face="Arial">1/2/2012</font></td>
			 *  <td align="middle"> <font size="1" color="#0000FF" face="Arial">31/1/2014</font></td>
			 *  <td align="middle"> <font size="1" color="#0000FF" face="Arial">NO</font></td>
			 * </tr>
			 */
      List<Dipendente> listaDipendenti = Lists.newArrayList();
      Elements anchorMatricole = listaDipendentiDoc.select("a[href*=DettDip?matr=]");
      for (Element e : anchorMatricole) {
        String matricola = e.ownText();
        Element tdMatricola = e.parent().parent().parent();
        //The HTML entity &nbsp; (Unicode character NO-BREAK SPACE U+00A0) can in Java be represented by the character \u00a0
        String nomeCognome = tdMatricola.siblingElements().get(1).text().replace("\u00a0", "").trim();
        Logger.debug("Nel html della lista delle persone individuato \"%s\", matricola=\"%s\"", nomeCognome, matricola);
        Person person = persondao.getPersonByNumber(Integer.parseInt(matricola));
        listaDipendenti.add(new Dipendente(person, nomeCognome));
      }

      return listaDipendenti;

    } catch (IOException e) {
      Logger.error("Errore durante il prelevamento della lista dei dipendneti. Eccezione = %s", e.getStackTrace().toString());
      throw new AttestatiException(
              String.format("Errore durante il prelevamento della lista dei dipendneti. Eccezione = %s", e.getStackTrace().toString()));
    }
  }

  public RispostaElaboraDati elaboraDatiDipendente(
          Map<String, String> cookies, Dipendente dipendente, Integer year, Integer month,
          List<Absence> absences, List<Competence> competences, List<PersonMonthRecap> pmList, Integer mealTicket)
          throws URISyntaxException, MalformedURLException {

    //Configuration conf = Configuration.getCurrentConfiguration();
    //ConfGeneral conf = ConfGeneral.getConfGeneral();
    Office office = Security.getUser().get().person.office;
    String urlToPresence = confGeneralManager.getFieldValue(Parameter.URL_TO_PRESENCE, office);

    URI baseUri = new URI(urlToPresence);
    final URL elaboraDatiUrl = baseUri.resolve(BASE_ELABORA_DATI_URL).toURL();

    StringBuffer absencesSent = new StringBuffer();
    StringBuffer competencesSent = new StringBuffer();
    //Nuovo stringBuffer per l'invio delle ore di formazione
    StringBuffer trainingHoursSent = new StringBuffer();
    StringBuffer mealTicketSent = new StringBuffer();
    StringBuffer problems = new StringBuffer();

    boolean isOk = true;

    Connection connection = Jsoup.connect(elaboraDatiUrl.toString());
    connection.cookies(cookies);

    connection.userAgent(CLIENT_USER_AGENT)
            .data("matr", dipendente.getMatricola())
            .data("anno", year.toString())
            .data("mese", month.toString())
            .data("sede_id", office.codeId)
            .method(Method.POST);

    int codAssAssoCounter = 0;
    for (AssenzaPerPost assenzaPerPost : getAssenzePerPost(absences)) {

      connection.data("codass" + codAssAssoCounter, assenzaPerPost.getCodice());
      connection.data("gg_inizio" + codAssAssoCounter, assenzaPerPost.getGgInizio().toString());
      connection.data("gg_fine" + codAssAssoCounter, assenzaPerPost.getGgFine().toString());
      absencesSent.append(assenzaPerPost.getCodice()).append(",")
              .append(assenzaPerPost.getGgInizio()).append(",")
              .append(assenzaPerPost.getGgFine()).append("; ");
      Logger.info("%s, sto spedendo l'assenza di tipo %s, gg inizio = %d, gg_fine = %d",
              dipendente.getCognomeNome(), assenzaPerPost.getCodice(),
              assenzaPerPost.getGgInizio(), assenzaPerPost.getGgInizio());
      codAssAssoCounter++;
    }

    int codComCounter = 0;
    for (Competence competence : competences) {
      connection.data("codcom" + codComCounter, competence.competenceCode.code);
      connection.data("oreatt" + codComCounter, String.valueOf(competence.valueApproved));
      competencesSent.append(competence.competenceCode.code).append(",")
              .append(competence.valueApproved).append("; ");
      Logger.info("%s, sto spedendo la competenza di tipo %s, ore attribuite = %d",
              dipendente.getCognomeNome(), competence.competenceCode.code, competence.valueApproved);
      codComCounter++;
    }

    int codFormCounter = 0;
    if (pmList != null) {
      for (PersonMonthRecap pm : pmList) {
        connection.data("gg_inizio_corso" + codFormCounter, String.valueOf(pm.fromDate.getDayOfMonth()));
        connection.data("gg_fine_corso" + codFormCounter, String.valueOf(pm.toDate.getDayOfMonth()));
        connection.data("ore_corso" + codFormCounter, String.valueOf(pm.trainingHours));
        trainingHoursSent.append(String.valueOf(pm.fromDate.getDayOfMonth())).append(",")
                .append(String.valueOf(pm.toDate.getDayOfMonth())).append(",")
                .append(String.valueOf(pm.trainingHours)).append("; ");
        Logger.info("%s, sto spedendo %d ore di formazione dal giorno %s al giorno %s", dipendente.getCognomeNome(), pm.trainingHours,
                pm.fromDate, pm.toDate);
        codFormCounter++;
      }
    }

    // Decommentato in virtù dell'aggiornamento dovuto all'utilizzo dei ticket restaurant
    if (mealTicket != null) {
      connection.data("gg_buoni_pasto", String.valueOf(mealTicket));
      mealTicketSent.append(String.valueOf(year)).append(",").append(String.valueOf(month)).append(",")
              .append(String.valueOf(mealTicket));
      Logger.info("Inviati %d buoni pasto per %s", mealTicket, dipendente.getCognomeNome());
    }


    Response elaboraDatiResponse;
    try {
      elaboraDatiResponse = connection.execute();

      Logger.debug("Effettuata l'elaborazione dati del dipendente %s (matricola %s) per l'anno %d, mese %d. Codice di risposta http = %d",
              dipendente.getCognomeNome(), dipendente.getMatricola(), year, month, elaboraDatiResponse.statusCode());

      if (elaboraDatiResponse.statusCode() != 200) {
        throw new AttestatiException(String.format("Errore durante l'elaborazione dati del dipendente %s", dipendente.getCognomeNome()));
      }

      Document elaboraDatiDoc = elaboraDatiResponse.parse();
      Logger.info("Risposta all'elaborazione dati = \n%s", elaboraDatiDoc);

			/*
			 * In caso di errore nella pagina restituita compaiono degli H5 come questi:
			 *	<H5 align=center><FONT SIZE='4' FACE='Arial'>Errore in fase di controllo competenze <BR>7  ERRASSSOVRAPP<BR>Assenza  OA7 in periodi sovrapposti </FONT></H5>
			 *	<BR>Controllo Competenze --> ..Effettuato!
			 *	<B>Non sono state inserite competenze</B>
			 *	<H5 align=center><FONT SIZE='4' FACE='Arial'>Errore in fase di controllo assenze dipendente=9535, mese=10, anno=2013, errore=7  ERRASSSOVRAPP<BR>Assenza  OA7 in periodi sovrapposti </FONT></H5>
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
        //Si aggiunge il contenuto testuale degli elementi font che contengono il messaggio di errore
        for (Element el : errorElements) {
          problems.append(el.ownText());
        }
        isOk = false;
      }
    } catch (IOException e) {
      Logger.error("Errore la chiamata alla funzione \"elabora dati\" sistema di invio degli attestati. Eccezione = %s", e.getStackTrace().toString());
      throw new AttestatiException(String.format("Impossibile effettuare l'elaborazione dati su %s", elaboraDatiUrl));
    }

    RispostaElaboraDati resp = new RispostaElaboraDati(dipendente.getCognomeNome(), dipendente.getMatricola());
    resp.setAbsencesSent(absencesSent.length() > 0 ? absencesSent.toString() : null);
    resp.setProblems(problems.length() > 0 ? problems.toString() : null);
    resp.setCompetencesSent(competencesSent.length() > 0 ? competencesSent.toString() : null);
    resp.setTrainingHoursSent(trainingHoursSent.length() > 0 ? trainingHoursSent.toString() : null);
    resp.setMealTicketSent(mealTicketSent.length() > 0 ? mealTicketSent.toString() : null);
    resp.setOk(isOk);
    return resp;
  }

  /**
   * Contenitore dei dati necessari per l'invio dei periodi di assenza del personale tramite il
   * sistema degli attestati del CNR.
   *
   * @author cristian
   */
  private final static class AssenzaPerPost {
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

  @SuppressWarnings("serial")
  public final static class LoginResponse implements Serializable {
    private final boolean loggedIn;
    private final Map<String, String> cookies;
    private String usernameCnr;
    private Integer year;
    private Integer month;

    public LoginResponse(String usernameCnr, boolean loggedIn, Map<String, String> cookies, Integer year, Integer month) {
      this.usernameCnr = usernameCnr;
      this.loggedIn = loggedIn;
      this.cookies = cookies;
      this.year = year;
      this.month = month;
    }

    public LoginResponse(String usernameCnr, boolean loggedIn, Map<String, String> cookies, Integer year, Integer month, Exception e) {
      this.usernameCnr = usernameCnr;
      this.loggedIn = loggedIn;
      this.cookies = cookies;
      this.year = year;
      this.month = month;
    }

    public String getUsernameCnr() {
      return usernameCnr;
    }

    public boolean isLoggedIn() {
      return loggedIn;
    }

    public Map<String, String> getCookies() {
      return cookies;
    }

    public Integer getYear() {
      return this.year;
    }

    public Integer getMonth() {
      return this.month;
    }

    public String getNamedMonth() {
      return DateUtility.fromIntToStringMonth(this.month);
    }
  }
}
