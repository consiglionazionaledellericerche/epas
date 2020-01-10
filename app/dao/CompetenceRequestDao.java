package dao;

import java.util.List;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import org.joda.time.LocalDateTime;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Provider;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPQLQueryFactory;
import models.Person;
import models.flows.AbsenceRequest;
import models.flows.CompetenceRequest;
import models.flows.enumerate.AbsenceRequestType;
import models.flows.enumerate.CompetenceRequestType;
import models.flows.query.QAbsenceRequest;
import models.flows.query.QCompetenceRequest;

public class CompetenceRequestDao extends DaoBase {

  @Inject
  CompetenceRequestDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }
  
  /**
   * Lista delle richiesta di assenza per persona e data.
   *
   * @param person La persona della quale recuperare le richieste di assenza
   * @param fromDate La data iniziale dell'intervallo temporale da considerare
   * @param toDate La data finale dell'intervallo temporale da considerare (opzionale)
   * @param absenceRequestType Il tipo di richiesta di assenza specifico
   * @return La lista delle richieste di assenze sull'intervallo e la persona specificati.
   */
  public List<CompetenceRequest> findByPersonAndDate(Person person,
      LocalDateTime fromDate, Optional<LocalDateTime> toDate,
      CompetenceRequestType competenceRequestType, boolean active) {

    Preconditions.checkNotNull(person);
    Preconditions.checkNotNull(fromDate);

    final QCompetenceRequest competenceRequest = QCompetenceRequest.competenceRequest;

    BooleanBuilder conditions = new BooleanBuilder(competenceRequest.person.eq(person)
        .and(competenceRequest.startAt.after(fromDate))
        .and(competenceRequest.type.eq(competenceRequestType)));
    if (toDate.isPresent()) {
      conditions.and(competenceRequest.endTo.before(toDate.get()));
    }
    if (active) {
      conditions.and(competenceRequest.flowEnded.eq(false));
    } else {
      conditions.and(competenceRequest.flowEnded.eq(true));
    }
    return getQueryFactory().selectFrom(competenceRequest)
        .where(conditions).orderBy(competenceRequest.startAt.desc()).fetch();
  }

}
