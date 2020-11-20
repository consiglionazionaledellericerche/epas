package manager.ldap;

import com.google.common.base.Optional;
import java.io.IOException;
import java.util.Hashtable;
import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.StartTlsRequest;
import javax.naming.ldap.StartTlsResponse;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import play.Play;

/**
 * Gestire la connessione e l'interrogazione dell'LDAP per
 * l'autenticazione degli utenti.
 * 
 * @author Cristian Lucchesi
 *
 */
@Slf4j
public class LdapService {

  private static final String ldapUrl = Play.configuration.getProperty("ldap.url");

  private static final boolean ldapStartTls = 
      Boolean.parseBoolean(Play.configuration.getProperty("ldap.startTls", "false"));

  private static final String adminPrincipal = 
      Play.configuration.getProperty("ldap.admin.principal");
  private static final String adminCredentials = 
      Play.configuration.getProperty("ldap.admin.credentials");
  
  public static final String ldapUniqueIdentifier =
      Play.configuration.getProperty("ldap.uniqueIdentifier", "uid");

  private static final Optional<String> ldapUniqueIdentifierPostfix = Optional.fromNullable(
      Play.configuration.getProperty("ldap.uniqueIdentifier.postfix"));

  //ou=People,dc=iit,dc=cnr,dc=it
  private static final String baseDn = Play.configuration.getProperty("ldap.dn.base");

  private static final int timeout =
      Integer.parseInt(Play.configuration.getProperty("ldap.timeout", "1000"));

  private static final boolean bindWithOnlyUid =
      Boolean.parseBoolean(Play.configuration.getProperty("ldap.bind.useOnlyUid", "false"));

  /**
   * Autenticazione tramite LDAP.
   *
   * @param username utente ldap
   * @param password password ldap
   * @return l'utente Ldap con i suoi attributi
   */
  public Optional<LdapUser> authenticate(String username, String password) {
    log.info("LDAP authentication -> autenticazione LDAP in corso per username {}. "
        + "LdapUrl = {}. StartTLS = {}, adminPrincipal = {}, adminCredentials = {}", 
        username, ldapUrl, ldapStartTls, adminPrincipal, adminCredentials);

    // Variabili usate solo se viene effettuata la prima connessione da 
    // utente amministatore 
    LdapContext authAdminContext = null;
    Optional<StartTlsResponse> tlsAdmin = Optional.absent();

    // Se è impostato un utente LDAP amministratore la prima connessione viene
    // effettuata con questo utente.
    if (adminPrincipal != null && adminCredentials != null) {
      log.info("LDAP authentication -> Effettuo la prima connessione LDAP con "
          + "utente admin = {}", adminPrincipal);

      val authAdminEnv = baseAuthEnv();

      try {
        authAdminContext = new InitialLdapContext(authAdminEnv, null);

        tlsAdmin = startTlsIfNecessary(authAdminContext);
        addAuthInfo(authAdminContext, adminPrincipal, adminCredentials);

      } catch (Exception e) {
        log.error("LDAP authentication -> connection using admin user {} failed. "
            + "Something went wrong during LDAP authentication for username = {}",
            adminPrincipal, username, e);
        return Optional.absent();
      }
    }

    val ldapUser = authenticateUser(username, password);

    try {
      // Le due close successive sono solo nel caso ci sia autenticati 
      // preventivamente con un utente admin di LDAP
      if (tlsAdmin.isPresent()) {
        tlsAdmin.get().close();
      }
      if (authAdminContext != null) {
        authAdminContext.close();
      }      
    } catch (Exception ex) {
      log.error("LDAP authentication -> something went wrong during LDAP admin "
          + "connection closing for {}={}", ldapUniqueIdentifier, username, ex);
      return Optional.absent();
    }

    return ldapUser;
  }

  /**
   * Effettua una ricerca dell'utente su LDAP effettuando la query con le
   * credenziali dell'utente per verificare che siano corrispondenti a quelle
   * LDAP.
   * 
   * @param username username dell'utente da autenticare
   * @param password password dell'utente da autenticare
   * 
   * @return un LdapUser con le informazioni dell'utente, Optional.absent() se
   *     se l'autenticazione non va a buon fine.
   */
  private Optional<LdapUser> authenticateUser(String username, String password) {
    
    val authEnv = baseAuthEnv();
    String dn = userDn(username);
    
    Optional<LdapUser> ldapUser = Optional.absent(); 
    LdapContext authContext = null;
    Optional<StartTlsResponse> tls = Optional.absent();
    
    try {
      authContext = new InitialLdapContext(authEnv, null);    

      startTlsIfNecessary(authContext);
      addAuthInfo(authContext, dn, password);

      val ldapSearchResult = 
          authContext.search(
              baseDn, String.format("(%s=%s)", ldapUniqueIdentifier, username), 
              searchControls());

      ldapUser = userFromSearchResults(username, ldapSearchResult);

    } catch (AuthenticationException authEx) {
      log.info("LDAP authentication -> Authentication failed for {}. dn={}",
          username, dn, authEx);
    } catch (Exception ex) {
      log.error("LDAP authentication -> something went wrong during LDAP authentication "
          + "for {}={}, dn = {}", ldapUniqueIdentifier, username, dn, ex);
    } finally {
      try {
        if (tls.isPresent()) {
          tls.get().close();
        }
        if (authContext != null) {
          authContext.close();
        }
      } catch (Exception e) {
        log.error("LDAP authentication -> something went wront during LDAP connection closing");
      }
    }
    return ldapUser;
  }

  private Optional<StartTlsResponse> startTlsIfNecessary(LdapContext authContext) 
      throws NamingException, IOException {
    StartTlsResponse tls = null;
    if (ldapStartTls) {
      // Start TLS
      log.info("LDAP authentication -> starting TLS ...");
      tls = (StartTlsResponse) authContext.extendedOperation(new StartTlsRequest());
      tls.negotiate();
      log.info("LDAP authentication -> ....negoziazione TLS avvenuta");
      return Optional.of(tls);
    }
    return Optional.absent();
  }
  
  /**
   * Costruisce le informazioni di ricerca da passare alla query LDAP per prelevare
   * le info degli utenti.
   * @return SearchControls per prelevare gli attributi dell'utente.
   */
  private SearchControls searchControls() {
    SearchControls ctrls = new SearchControls();
    ctrls.setReturningAttributes(
        new String[]{ldapUniqueIdentifier, "givenName", "sn", "mail",
            getEppnAttributeName()});
    ctrls.setSearchScope(SearchControls.SUBTREE_SCOPE); 
    return ctrls;
  }
  
  /**
   * Costruisce un LdapUser dai risultati della ricerca LDAP.
   * @param username lo username dell'utente di cui costruire i dati
   * @param ldapSearchResult il risultato della query LDAP
   * @return un LdapUser con le info dell'utente prelevate da LDAP se presente, 
   *     Optional.absent() altrimenti.
   * @throws NamingException  sollevata per problemi di accesso ai campi ldap
   */
  private Optional<LdapUser> userFromSearchResults(
      String username, NamingEnumeration<SearchResult> ldapSearchResult) throws NamingException {
    if (ldapSearchResult == null || !ldapSearchResult.hasMoreElements()) {
      log.info("LDAP authentication -> LdapSearch failed for {}={} using baseDn={}",
          ldapUniqueIdentifier, username, baseDn);
      return Optional.absent();
    }
    log.info("LDAP authentication -> LDAP Authentication Success for {}", username);
    SearchResult result = ldapSearchResult.nextElement();

    return Optional.of(LdapUser.create(result.getAttributes(), getEppnAttributeName()));

  }
  
  /**
   * Mappa con l'Environment di base comune.
   * @return una mappa con l'Environment di base comune per le connessioni LDAP
   */
  private Hashtable<String, String> baseAuthEnv() {
    val authEnv = new Hashtable<String, String>();
    authEnv.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
    authEnv.put("com.sun.jndi.ldap.connect.timeout", "" + timeout * 1000);
    authEnv.put(Context.PROVIDER_URL, ldapUrl);
    return authEnv;
  } 

  /**
   * Aggiunge all'LdapContext il tipo di autenticazione, l'utente e la password.
   */
  private void addAuthInfo(
      LdapContext authContext, String principal, String credentials) throws NamingException {
    authContext.addToEnvironment(Context.SECURITY_AUTHENTICATION, "simple");
    authContext.addToEnvironment(Context.SECURITY_PRINCIPAL, principal);
    authContext.addToEnvironment(Context.SECURITY_CREDENTIALS, credentials);   
  }
  
  /**
   * Costruisce il distinguished name (DN) dell'utente in funzione dei parametri LDAP.
   * 
   * @param username username dell'utente
   * @return dn dell'utente da utilizzare nelle query ldap
   */
  private String userDn(String username) {
    String usernameForBind = username;
    if (ldapUniqueIdentifierPostfix.isPresent()) {
      usernameForBind += ldapUniqueIdentifierPostfix.get();
    }

    String dn = bindWithOnlyUid
        ? usernameForBind : ldapUniqueIdentifier + "=" + usernameForBind + "," + baseDn;
    log.info("LDAP authentication -> DN dell'utente utilizzato per il login: {}", dn);
    return dn;
  }
  
  /**
   * Utilizzato per decidere qualche attributo LDAP utilizzare per fare il mapping con l'attributo
   * eppn presente in ePAS.
   *
   * @return nome del campo utilizzato per fare il match tra gli utenti LDAP e quelli di ePAS.
   */
  public String getEppnAttributeName() {
    return Play.configuration.getProperty("ldap.eppn.attribute.name", "eduPersonPrincipalName");
  }
}