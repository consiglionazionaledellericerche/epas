package models.base;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitJoinTableNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;

/**
 * Clone di quello spring-boot.
 *
 * @author marco
 *
 */
public class OurImplicitNamingStrategy extends ImplicitNamingStrategyJpaCompliantImpl {

  private static final long serialVersionUID = -721875022035961973L;

  @Override
  public Identifier determineJoinTableName(ImplicitJoinTableNameSource source) {
       String name = source.getOwningPhysicalTableName() + "_"
               + source.getAssociationOwningAttributePath().getProperty();
       return toIdentifier(name, source.getBuildingContext());
  }
}
