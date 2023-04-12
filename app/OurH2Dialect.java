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

import java.sql.Types;

//This is to address the mapping exception when using uuid data type in
//PostgreSQL with Hibernate
//javax.persistence.PersistenceException: org.hibernate.MappingException:
//  No Dialect mapping for JDBC type: 1111
//This makes the assumption that no other fields types will be using the java.sql.Types.OTHER
//data type
/**
 * Dialetto H2 con alcune estensioni utili per i test.
 *
 * @author cristian
 *
 */
public class OurH2Dialect extends org.hibernate.dialect.H2Dialect {

  /**
   * Registra alcuni tipi di base (varchar e string).
  */
  public OurH2Dialect() {
    super();
    registerColumnType(Types.OTHER, "varchar");
    registerHibernateType(Types.OTHER, "string");
  }
}
