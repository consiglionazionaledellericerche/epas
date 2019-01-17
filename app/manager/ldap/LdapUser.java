package manager.ldap;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

/**
 * DTO per le informazioni di base prelevate da LDAP.
 * 
 * @author cristian
 *
 */
@Data 
@AllArgsConstructor
@ToString
public class LdapUser {

  private String uid;
  private String givenName;
  private String sn;
  private String mail;
  
  /**
   * Builder from LDAP attributes.
   * 
   * @param attributes ldap attributes.
   * @return un LDAPUser con i valori estratti dagli attributi passati.
   * @throws NamingException sollevata nel caso non ci siano tutti gli attributi LDAP richiesti
   */
  public static LdapUser create(Attributes attributes) throws NamingException {
    return new LdapUser(
        attributes.get("uid").get().toString(), attributes.get("givenName").get().toString(), 
        attributes.get("sn").get().toString(), attributes.get("mail").get().toString());
  }
}
