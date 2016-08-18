package dao;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

import dao.wrapper.IWrapperFactory;

import models.Competence;
import models.CompetenceCode;
import models.Office;
import models.Person;
import models.PersonHourForOvertime;
import models.PersonReperibilityType;
import models.TotalOvertime;
import models.query.QCompetence;
import models.query.QCompetenceCode;
import models.query.QPerson;
import models.query.QPersonCompetenceCodes;
import models.query.QPersonHourForOvertime;
import models.query.QPersonReperibility;
import models.query.QPersonReperibilityType;
import models.query.QPersonShiftShiftType;
import models.query.QTotalOvertime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import javax.persistence.EntityManager;

/**
 * @author dario
 */
public class CompetenceDao extends DaoBase {

  private static final Logger log = LoggerFactory.getLogger(CompetenceDao.class);

  @Inject
  CompetenceDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp,
      IWrapperFactory wrapperFactory) {
    super(queryFactory, emp);
  }

  /**
   * @return la competenza relativa all'id passato come parametro.
   */
  public Competence getCompetenceById(Long id) {

    final QCompetence competence = QCompetence.competence;

    return getQueryFactory().from(competence)
            .where(competence.id.eq(id)).singleResult(competence);
  }

  /**
   * La lista dei CompetenceCode abilitati ad almeno una persona appartenente all'office.
   */
  public List<CompetenceCode> activeCompetenceCode(Office office) {

    final QCompetenceCode competenceCode = QCompetenceCode.competenceCode;
    final QPersonCompetenceCodes pcc = QPersonCompetenceCodes.personCompetenceCodes;
    return getQueryFactory().from(competenceCode)
        .leftJoin(competenceCode.personCompetenceCodes, pcc).fetch()
            .where(pcc.person.office.eq(office)).orderBy(competenceCode.code.asc())
            .distinct().list(competenceCode);
  }

  /**
   * 
   * @param person
   * @param year
   * @param month
   * @param codes
   * @return la lista di competenze appartenenti alla lista di codici codes relative all'anno year 
   *     e al mese month per la persona person.
   */
  public List<Competence> getCompetences(
      Optional<Person> person, Integer year, Optional<Integer> month, List<CompetenceCode> codes) {

    final QCompetence competence = QCompetence.competence;
    final BooleanBuilder condition = new BooleanBuilder();
    condition.and(competence.year.eq(year)
        .and(competence.competenceCode.in(codes)));
    if (month.isPresent()) {
      condition.and(competence.month.eq(month.get()));
    }
    if (person.isPresent()) {
      condition.and(competence.person.eq(person.get()));
    }
    final JPQLQuery query = getQueryFactory().from(competence)
            .leftJoin(competence.competenceCode).fetch()
            .where(condition);

    return query.list(competence);
  }

  /**
   * 
   * @param person
   * @param year
   * @param month
   * @param code
   * @return la competenza se esiste relativa all'anno year e al mese month con codice code per la 
   * persona person.
   */
  public Optional<Competence> getCompetence(
      Person person, Integer year, Integer month, CompetenceCode code) {

    final QCompetence competence = QCompetence.competence;

    final JPQLQuery query = getQueryFactory().from(competence)
            .where(competence.person.eq(person)
                    .and(competence.year.eq(year)
                            .and(competence.month.eq(month)
                                    .and(competence.competenceCode.eq(code)))));

    return Optional.fromNullable(query.singleResult(competence));

  }

  /**
   * La lista delle competence assegnate nell'office.
   *
   * <p>
   * Se untilThisMonth è true, viene presa la lista delle competenze dall'inizio dell'anno fino a
   * quel mese compreso, se è false solo quelle del mese specificato.
   * </p>
   *
   * @param codes filtra i codici di competenza.
   * @param office filtra per persone dell'office.
   */
  public List<Competence> getCompetencesInOffice(
      Integer year, Integer month, List<String> codes, Office office, boolean untilThisMonth) {

    final QCompetence competence = QCompetence.competence;
    final BooleanBuilder condition = new BooleanBuilder();

    condition.and(competence.year.eq(year))
            .and(competence.competenceCode.code.in(codes))
            .and(competence.person.office.eq(office));

    if (untilThisMonth) {
      condition.and(competence.month.loe(month));
    } else {
      condition.and(competence.month.eq(month));
    }

    return getQueryFactory().from(competence)
            .where(condition).list(competence);
  }

  /**
   * Le competenze nell'anno year. Se office è present filtra sulle sole competenze assegnate alle
   * persone nell'office.
   */
  public List<Competence> getCompetenceInYear(Integer year, Optional<Office> office) {

    final QCompetence competence = QCompetence.competence;
    final BooleanBuilder condition = new BooleanBuilder();

    condition.and(competence.year.eq(year));

    if (office.isPresent()) {
      condition.and(competence.person.office.eq(office.get()));
    }

    return getQueryFactory().from(competence)
            .where(condition).orderBy(competence.competenceCode.code.asc())
            .list(competence);
  }

  /**
   * @return sulla base dei parametri passati alla funzione ritorna la quantità di ore approvate di
   *     straordinario (sommando i codici S1 S2 e S3).
   */
  public Optional<Integer> valueOvertimeApprovedByMonthAndYear(
      Integer year, Optional<Integer> month, Optional<Person> person,
      List<CompetenceCode> codeList) {

    final QCompetence competence = QCompetence.competence;
    final BooleanBuilder condition = new BooleanBuilder();

    if (month.isPresent()) {
      condition.and(competence.month.eq(month.get()));
    }
    if (person.isPresent()) {
      condition.and(competence.person.eq(person.get()));
    }
    final JPQLQuery query =
        getQueryFactory()
          .from(competence)
          .where(condition.and(competence.year.eq(year)
                      .and(competence.competenceCode.in(codeList))));

    return Optional.fromNullable(query.singleResult(competence.valueApproved.sum()));

  }


  /**
   * @return la lista di tutte le competenze di una persona nel mese month e nell'anno year che
   *     abbiano un valore approvato > 0.
   */
  public List<Competence> getAllCompetenceForPerson(Person person, Integer year, Integer month) {
    return competenceInMonth(person, year, month, Optional.<List<String>>absent());
  }


  public List<Competence> competenceInMonth(
      Person person, Integer year, Integer month, Optional<List<String>> codes) {

    final QCompetence competence = QCompetence.competence;
    final BooleanBuilder condition = new BooleanBuilder();

    condition.and(competence.year.eq(year))
            .and(competence.person.eq(person))
            .and(competence.month.eq(month).and(competence.valueApproved.gt(0)));

    if (codes.isPresent()) {
      condition.and(competence.competenceCode.code.in(codes.get()));
    }

    return getQueryFactory().from(competence)
            .where(condition).list(competence);
  }

  /**
   * metodo di utilità per il controller UploadSituation.
   *
   * @return la lista delle competenze del dipendente in questione per quel mese in quell'anno
   */
  public List<Competence> getCompetenceInMonthForUploadSituation(
      Person person, Integer year, Integer month) {
    List<Competence> competenceList = getAllCompetenceForPerson(person, year, month);

    log.trace("Per la persona {} trovate {} competenze approvate nei mesi di {}/{}",
            new Object[]{person.getFullname(), competenceList.size(), month, year});

    return competenceList;
  }

  /**
   * @return la lista di competenze relative all'anno year, al mese month e al codice code di
   *     persone che hanno reperibilità di tipo type associata.
   */
  public List<Competence> getCompetenceInReperibility(
      PersonReperibilityType type, int year, int month, CompetenceCode code) {
    final QCompetence competence = QCompetence.competence;
    final QPerson person = QPerson.person;
    final QPersonReperibilityType prt = QPersonReperibilityType.personReperibilityType;

    JPQLQuery query = getQueryFactory().from(competence)
            .leftJoin(competence.person, person)
            .leftJoin(person.reperibility.personReperibilityType, prt)
            .where(prt.eq(type)
                    .and(competence.year.eq(year)
                            .and(competence.month.eq(month)
                                    .and(competence.competenceCode.eq(code)))))
            .orderBy(competence.person.surname.asc());

    return query.list(competence);
  }


  /**
   * @return l'ultima competenza assegnata din un certo typo in un determinato anno.
   */
  public Competence getLastPersonCompetenceInYear(
      Person person, int year, int month, CompetenceCode competenceCode) {
    final QCompetence com = new QCompetence("competence");
    final JPQLQuery query = getQueryFactory().query();
    final Competence myCompetence = query
            .from(com)
            .where(
                    com.person.eq(person)
                            .and(com.year.eq(year))
                            .and(com.month.lt(month))
                            .and(com.competenceCode.eq(competenceCode))
            )
            .orderBy(com.month.desc())
            .limit(1)
            .uniqueResult(com);

    return myCompetence;
  }
  
  /**
   * 
   * @param code il codice competenza da cercare
   * @return la lista di tutte le competenze che contengono quel codice competenza.
   */
  public List<Competence> findCompetence(CompetenceCode code) {
    final QCompetence comp = QCompetence.competence;
    final JPQLQuery query = getQueryFactory().from(comp).where(comp.competenceCode.eq(code));
    return query.list(comp);
  }


  /**
   * @return dei quantitativi di straordinario assegnati per l'ufficio office nell'anno year.
   */
  public List<TotalOvertime> getTotalOvertime(Integer year, Office office) {
    final QTotalOvertime totalOvertime = QTotalOvertime.totalOvertime;

    return getQueryFactory().from(totalOvertime)
            .where(totalOvertime.year.eq(year).and(totalOvertime.office.eq(office)))
            .list(totalOvertime);
  }

  /**
   * @return il personHourForOvertime relativo alla persona person passata come parametro.
   */
  public PersonHourForOvertime getPersonHourForOvertime(Person person) {

    final QPersonHourForOvertime personHourForOvertime =
        QPersonHourForOvertime.personHourForOvertime;

    return getQueryFactory().from(personHourForOvertime)
            .where(personHourForOvertime.person.eq(person))
            .singleResult(personHourForOvertime);
  }

}
