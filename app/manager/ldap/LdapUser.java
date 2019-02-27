package manager.ldap;

import com.google.common.base.Optional;
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
  private String eppn;
  
  /**
   * Builder from LDAP attributes.
   * 
   * @param attributes ldap attributes.
   * @param eppnAttributeName nome dell'attributo LDAP da utilizzate per
   *        prelevare il campo eppn di questo utente.
   * @return un LDAPUser con i valori estratti dagli attributi passati.
   * @throws NamingException sollevata nel caso non ci siano tutti gli attributi LDAP richiesti
   */
  public static LdapUser create(Attributes attributes, String eppnAttributeName) 
      throws NamingException {    
    return new LdapUser(
        attributes.get(LdapService.ldapUniqueIdentifier).get().toString(), 
        attributes.get("givenName") != null ? attributes.get("givenName").get().toString() : null, 
        attributes.get("sn") != null ? attributes.get("sn").get().toString() : null, 
        attributes.get("mail") != null ? attributes.get("mail").get().toString() : null,
        attributes.get(eppnAttributeName) != null 
          ? attributes.get(eppnAttributeName).get().toString() : null);
  }
}
