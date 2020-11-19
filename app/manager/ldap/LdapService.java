package manager.ldap;

import com.google.common.base.Optional;
import java.util.Hashtable;
import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.StartTlsRequest;
import javax.naming.ldap.StartTlsResponse;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import play.Play;

@Slf4j
public class LdapService {

  //ldap://ldap.iit.cnr.it:389

  public static final String ldapUrl = Play.configuration.getProperty("ldap.url");

  public static final boolean ldapStartTls = 
      Boolean.parseBoolean(Play.configuration.getProperty("ldap.startTls", "false"));
  
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
   * Esempio autenticazione LDAP.
   *
   * @param username utente ldap
   * @param password password ldap
   * @return l'email dell'utente se autenticato e se l'email è presente.
   */
  public Optional<LdapUser> authenticate(String username, String password) {
    val authEnv = new Hashtable<String, String>();

    String usernameForBind = username;
    if (ldapUniqueIdentifierPostfix.isPresent()) {
      usernameForBind += ldapUniqueIdentifierPostfix.get();
    }

    String dn = bindWithOnlyUid
        ? usernameForBind : ldapUniqueIdentifier + "=" + usernameForBind + "," + baseDn;

    authEnv.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
    authEnv.put("com.sun.jndi.ldap.connect.timeout", "" + timeout * 1000);

    authEnv.put(Context.PROVIDER_URL, ldapUrl);
    authEnv.put(Context.SECURITY_AUTHENTICATION, "none");
    //authEnv.put(Context.SECURITY_PRINCIPAL, dn);
    //authEnv.put(Context.SECURITY_CREDENTIALS, password);

    Optional<LdapUser> ldapUser = Optional.absent(); 

    SearchControls ctrls = new SearchControls();
    ctrls.setReturningAttributes(
        new String[]{ldapUniqueIdentifier, "givenName", "sn", "mail",
            getEppnAttributeName()});
    ctrls.setSearchScope(SearchControls.SUBTREE_SCOPE);

    try {
      LdapContext authContext = new InitialLdapContext(authEnv, null);

      StartTlsResponse tls = null;

      if (ldapStartTls) {
        // Start TLS
        log.info("Starting TLS...");
        tls = (StartTlsResponse) authContext.extendedOperation(new StartTlsRequest());
        tls.negotiate();
        log.info("....Negoziazione TLS avvenuta");
      }
      authContext.addToEnvironment(Context.SECURITY_AUTHENTICATION, "simple");
      authContext.addToEnvironment(Context.SECURITY_PRINCIPAL, dn);
      authContext.addToEnvironment(Context.SECURITY_CREDENTIALS, password);     
      
      NamingEnumeration<SearchResult> answers =
          authContext.search(baseDn, "(" + ldapUniqueIdentifier + "=" + username + ")", ctrls);
      if (answers == null || !answers.hasMoreElements()) {
        log.info("LdapSearch failed for {}={} using baseDn={}",
            ldapUniqueIdentifier, username, baseDn);
        return Optional.absent();
      }
      log.info("LDAP Authentication Success for {}", username);
      SearchResult result = answers.nextElement();

      ldapUser = Optional.of(LdapUser.create(result.getAttributes(), getEppnAttributeName()));
      if (ldapStartTls && tls != null) {
        tls.close();
      }
      authContext.close();
      
    } catch (AuthenticationException authEx) {
      log.info("LDAP Authentication failed for {}. {}={}, dn={}",
          usernameForBind, ldapUniqueIdentifier, username, dn, authEx);
    } catch (Exception ex) {
      log.error("Something went wrong during LDAP authentication for {}={}, dn = {}",
          ldapUniqueIdentifier, username, dn, ex);
    }

    return ldapUser;
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