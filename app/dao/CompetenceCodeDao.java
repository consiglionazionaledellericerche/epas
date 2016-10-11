package dao;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

import models.CompetenceCode;
import models.CompetenceCodeGroup;
import models.Office;
import models.Person;
import models.PersonCompetenceCodes;
import models.query.QCompetenceCode;
import models.query.QCompetenceCodeGroup;
import models.query.QPersonCompetenceCodes;

import org.joda.time.LocalDate;

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

    final JPQLQuery query = getQueryFactory().from(competenceCode)
        .where(competenceCode.disabled.eq(false));
    return query.orderBy(competenceCode.id.asc()).list(competenceCode);
  }
  
  /**
   * 
   * @return la lista dei gruppi di competenze presenti nel db.
   */
  public List<CompetenceCodeGroup> getAllGroups() {
    final QCompetenceCodeGroup group = QCompetenceCodeGroup.competenceCodeGroup;
    final JPQLQuery query = getQueryFactory().from(group);
    return query.orderBy(group.id.asc()).list(group);
  }
  
  /**
   * 
   * @param competenceCodeGroupId l'id del gruppo
   * @return il gruppo di codici competenza caratterizzato dall'id passato come parametro.
   */
  public CompetenceCodeGroup getGroupById(Long competenceCodeGroupId) {
    final QCompetenceCodeGroup group = QCompetenceCodeGroup.competenceCodeGroup;
    final JPQLQuery query = getQueryFactory().from(group).where(group.id.eq(competenceCodeGroupId));
    return query.singleResult(group);
  }
  
  /**
   * 
   * @return la lista di tutti i codici di competenza che non appartengono ad alcun gruppo.
   */
  public List<CompetenceCode> getCodeWithoutGroup() {
    final QCompetenceCode competenceCode = QCompetenceCode.competenceCode;
    final JPQLQuery query = getQueryFactory().from(competenceCode)
        .where(competenceCode.competenceCodeGroup.isNull());
    return query.orderBy(competenceCode.disabled.asc(), 
        competenceCode.code.asc()).list(competenceCode);
  }
  
  /**
   * 
   * @param group il gruppo di codici di competenza
   * @param except opzionale: se presente serve per tralasciare quel competenceCode nella ricerca
   * dei codici appartenenti al gruppo group
   * @return la lista dei codici di competenza che appartengono al gruppo passato come parametro.
   */
  public List<CompetenceCode> getCodeWithGroup(CompetenceCodeGroup group, 
      Optional<CompetenceCode> except) {
    final QCompetenceCode competenceCode = QCompetenceCode.competenceCode;
    BooleanBuilder condition = new BooleanBuilder();
    if (except.isPresent()) {
      condition.and(competenceCode.ne(except.get()));
    }
    final JPQLQuery query = getQueryFactory().from(competenceCode)
        .where(competenceCode.competenceCodeGroup.eq(group).and(condition));
    return query.orderBy(competenceCode.code.asc()).list(competenceCode);
  }
  
  /**
   * 
   * @param group il gruppo che si vuole inserire nella ricerca dei codici di competenza
   * @return la lista dei codici di competenza senza gruppo più quelli che appartengono
   *     al gruppo passato come parametro.
   */
  public List<CompetenceCode> allCodesContainingGroupCodes(CompetenceCodeGroup group) {
    final QCompetenceCode competenceCode = QCompetenceCode.competenceCode;
    final JPQLQuery query = getQueryFactory().from(competenceCode)
        .where(competenceCode.competenceCodeGroup.isNull()
            .or(competenceCode.competenceCodeGroup.eq(group)));
    return query.list(competenceCode);
  }
  
  /**
   * 
   * @param personlist la lista di persone di cui ritornare i codici di competenza abilitati
   * @return la lista di oggetti PersonCompetenceCodes. 
   */
//  public List<PersonCompetenceCodes> list(List<Person> personlist, LocalDate date) {
//    final QPersonCompetenceCodes pcc = QPersonCompetenceCodes.personCompetenceCodes;
//    final JPQLQuery query = getQueryFactory().from(pcc)
//        .where(pcc.person.in(personlist)
//        .and(pcc.beginDate.before(date)
//            .andAnyOf(pcc.endDate.isNull(), pcc.endDate.after(date)))).orderBy(pcc.person.surname.asc());
//    return query.list(pcc);
//  }
  
  /**
   * 
   * @param person la person
   * @return la lista di PersonCompetenceCodes associata alla persona passata come parametro.
   */
  public List<PersonCompetenceCodes> listByPerson(Person person, Optional<LocalDate> date) {
    final QPersonCompetenceCodes pcc = QPersonCompetenceCodes.personCompetenceCodes;
    final BooleanBuilder condition = new BooleanBuilder();
    if (date.isPresent()) {
      condition.and(pcc.beginDate.loe(date.get().dayOfMonth().withMaximumValue())
          .andAnyOf(pcc.endDate.isNull(), pcc.endDate.goe(date.get().dayOfMonth().withMaximumValue())));
    }
    final JPQLQuery query = getQueryFactory().from(pcc).where(pcc.person.eq(person).and(condition));
    return query.list(pcc);
  }
  
  /**
   * 
   * @param person
   * @param code
   * @return il PersonCompetenceCode relativo ai parametri passati, se presente.
   */
  public Optional<PersonCompetenceCodes> getByPersonAndCode(Person person, CompetenceCode code) {
    final QPersonCompetenceCodes pcc = QPersonCompetenceCodes.personCompetenceCodes;
    final JPQLQuery query = getQueryFactory().from(pcc)
        .where(pcc.person.eq(person).and(pcc.competenceCode.eq(code)));
    return Optional.fromNullable(query.singleResult(pcc));
  }
  
  /**
   * 
   * @param code
   * @param date
   * @param office
   * @return
   */
  public List<PersonCompetenceCodes> listByCompetenceCode(CompetenceCode code, 
      Optional<LocalDate> date, Office office) {
    final QPersonCompetenceCodes pcc = QPersonCompetenceCodes.personCompetenceCodes;
    final BooleanBuilder condition = new BooleanBuilder();
    if (date.isPresent()) {
      condition.and(pcc.beginDate.loe(date.get().dayOfMonth().withMaximumValue())
          .andAnyOf(pcc.endDate.isNull(), pcc.endDate.goe(date.get().dayOfMonth().withMaximumValue())));
    }
    final JPQLQuery query = getQueryFactory().from(pcc)
        .where(pcc.competenceCode.eq(code)
            .and(pcc.person.office.eq(office)).and(condition));
    return query.list(pcc);
  }
}
