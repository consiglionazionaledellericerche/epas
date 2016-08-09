package dao;

import com.google.inject.Inject;
import com.google.inject.Provider;

import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

import models.CompetenceCode;
import models.CompetenceCodeGroup;
import models.query.QCompetenceCode;
import models.query.QCompetenceCodeGroup;

import java.util.List;

import javax.persistence.EntityManager;

/**
 * @author dario
 */
public class CompetenceCodeDao extends DaoBase {

  @Inject
  CompetenceCodeDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * @return il competenceCode relativo al codice passato come parametro.
   */
  public CompetenceCode getCompetenceCodeByCode(String code) {
    final QCompetenceCode competenceCode = QCompetenceCode.competenceCode;

    final JPQLQuery query = getQueryFactory().from(competenceCode)
            .where(competenceCode.code.eq(code));

    return query.singleResult(competenceCode);

  }

  /**
   * @return il codice competenza relativo alla descrizione passata come parametro.
   */
  public CompetenceCode getCompetenceCodeByDescription(String description) {

    final QCompetenceCode competenceCode = QCompetenceCode.competenceCode;

    final JPQLQuery query = getQueryFactory().from(competenceCode)
            .where(competenceCode.description.eq(description));

    return query.singleResult(competenceCode);

  }

  /**
   * @return il codice di competenza relativo all'id passato come parametro.
   */
  public CompetenceCode getCompetenceCodeById(Long id) {

    final QCompetenceCode competenceCode = QCompetenceCode.competenceCode;

    final JPQLQuery query = getQueryFactory().from(competenceCode)
            .where(competenceCode.id.eq(id));

    return query.singleResult(competenceCode);

  }

  /**
   * @return la lista di tutti i codici di competenza presenti nel database.
   */
  public List<CompetenceCode> getAllCompetenceCode() {

    final QCompetenceCode competenceCode = QCompetenceCode.competenceCode;

    final JPQLQuery query = getQueryFactory().from(competenceCode);
    return query.orderBy(competenceCode.id.asc()).list(competenceCode);
  }
  
  /**
   * 
   * @return la lista dei gruppi di competenze presenti nel db.
   */
  public List<CompetenceCodeGroup> getAllGroups() {
    final QCompetenceCodeGroup group = QCompetenceCodeGroup.competenceCodeGroup;
    final JPQLQuery query = getQueryFactory().from(group);
    return query.orderBy(group.label.asc()).list(group);
  }
  
  /**
   * 
   * @return la lista di tutti i codici di competenza che non appartengono ad alcun gruppo.
   */
  public List<CompetenceCode> getCodeWithoutGroup() {
    final QCompetenceCode competenceCode = QCompetenceCode.competenceCode;
    final JPQLQuery query = getQueryFactory().from(competenceCode)
        .where(competenceCode.competenceCodeGroup.isNull());
    return query.orderBy(competenceCode.disabled.asc(), competenceCode.code.asc()).list(competenceCode);
  }
  
  /**
   * 
   * @param group il gruppo di codici di competenza
   * @return la lista dei codici di competenza che appartengono al gruppo passato come parametro.
   */
  public List<CompetenceCode> getCodeWithGroup(CompetenceCodeGroup group) {
    final QCompetenceCode competenceCode = QCompetenceCode.competenceCode;
    final JPQLQuery query = getQueryFactory().from(competenceCode)
        .where(competenceCode.competenceCodeGroup.eq(group));
    return query.orderBy(competenceCode.code.asc()).list(competenceCode);
  }
}
