/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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

  private final String uid;
  private final String mail;
  private final String eppn;
  private final String principal;
  
  /**
   * Builder from LDAP attributes.
   *
   * @param attributes ldap attributes.
   * @param eppnAttributeName nome dell'attributo LDAP da utilizzate per
   *        prelevare il campo eppn di questo utente.
   * @return un LDAPUser con i valori estratti dagli attributi passati.
   * @throws NamingException sollevata nel caso non ci siano tutti gli attributi LDAP richiesti
   */
  public static LdapUser create(String principal, Attributes attributes, String eppnAttributeName) 
      throws NamingException {    
    return new LdapUser(
        attributes.get(LdapService.ldapUniqueIdentifier).get().toString(), 
        attributes.get("mail") != null ? attributes.get("mail").get().toString() : null,
        attributes.get(eppnAttributeName) != null 
          ? attributes.get(eppnAttributeName).get().toString() : null,
        principal);
  }
}
