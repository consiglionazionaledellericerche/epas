import java.sql.Types;

//This is to address the mapping exception when using uuid data type in
//PostgreSQL with Hibernate
//javax.persistence.PersistenceException: org.hibernate.MappingException:
//  No Dialect mapping for JDBC type: 1111
//This makes the assumption that no other fields types will be using the java.sql.Types.OTHER
//data type
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
