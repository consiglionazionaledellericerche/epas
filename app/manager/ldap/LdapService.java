package manager.ldap;

import com.google.common.base.Optional;
import java.util.Hashtable;
import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import play.Play;

@Slf4j
public class LdapService {

  //ldap://ldap.iit.cnr.it:389
  
  public static final String ldapUrl = Play.configuration.getProperty("ldap.url");

  public static final String ldapUniqueIdentifier = 
      Play.configuration.getProperty("ldap.uniqueIdentifier", "uid");
  
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
  public static Optional<LdapUser> authenticate(String username, String password) {
    val authEnv = new Hashtable<String, String>();

    String dn = bindWithOnlyUid ? username : ldapUniqueIdentifier + "=" + username + "," + baseDn; 

    authEnv.put(Context.INITIAL_CONTEXT_FACTORY,"com.sun.jndi.ldap.LdapCtxFactory");
    authEnv.put("com.sun.jndi.ldap.connect.timeout", "" + (timeout * 1000));

    authEnv.put(Context.PROVIDER_URL, ldapUrl);
    authEnv.put(Context.SECURITY_AUTHENTICATION, "simple");
    authEnv.put(Context.SECURITY_PRINCIPAL, dn);
    authEnv.put(Context.SECURITY_CREDENTIALS, password);

    try {
      SearchControls ctrls = new SearchControls();
      ctrls.setReturningAttributes(
          new String[] { ldapUniqueIdentifier, "givenName", "sn", "mail", 
          getEppnAttributeName() });
      ctrls.setSearchScope(SearchControls.SUBTREE_SCOPE);
      
      DirContext authContext = new InitialDirContext(authEnv);
      log.info("LDAP Authentication Success for {}", username);
      
      NamingEnumeration<javax.naming.directory.SearchResult> answers = 
          authContext.search(baseDn, "(" + ldapUniqueIdentifier + "=" + username + ")", ctrls);
      if (answers == null || !answers.hasMoreElements()) {
        log.info("LdapSearch failed for {}={} using baseDn={}", 
            ldapUniqueIdentifier, username, baseDn);
        return Optional.absent();
      }
      javax.naming.directory.SearchResult result = answers.nextElement();      

      return Optional.of(LdapUser.create(result.getAttributes(), getEppnAttributeName()));
    } catch (AuthenticationException authEx) {
      log.info("LDAP Authentication failed for {}={}, dn = {}", 
          ldapUniqueIdentifier, username, dn, authEx);
      return Optional.absent();
    } catch (Exception ex) {
      log.error("Something went wrong during LDAP authentication for {}={}, dn = {}", 
          ldapUniqueIdentifier, username, dn, ex);
      return Optional.absent();
    }
  }
  
  /**
   * Utilizzato per decidere qualche attributo LDAP utilizzare per fare il mapping
   * con l'attributo eppn presente in ePAS. 
   * @return nome del campo utilizzato per fare il match tra gli utenti LDAP e quelli di ePAS.
   */
  public static String getEppnAttributeName() {
    return Play.configuration.getProperty("ldap.eppn.attribute.name", "eduPersonPrincipalName");
  }
}