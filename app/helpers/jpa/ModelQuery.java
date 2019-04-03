package helpers.jpa;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.querydsl.core.QueryModifiers;
import com.querydsl.core.QueryResults;
import com.querydsl.core.types.Expression;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import helpers.Paginator;
import java.util.List;
import models.base.BaseModel;
import play.db.jpa.JPA;
import play.mvc.Scope;

/**
 * @author marco
 */
public class ModelQuery {

  /**
   * Nome dell'argomento da fornire via request per indicare il numero di elementi per pagina.
   */
  private static final String PAGE_SIZE_PARAM = "limit";
  private static final long DEFAULT_PAGE_SIZE = 10L;

  private ModelQuery() {
  }

  public static JPQLQuery<?> createQuery() {
    return new JPAQuery(JPA.em());
  }

  public static JPQLQuery<?> paginatedQuery(JPQLQuery<?> query) {
    final Integer page = Optional.fromNullable(Scope.Params.current()
        .get(Paginator.PAGE_PARAM, Integer.class)).or(1);
    final long limit = Optional.fromNullable(Scope.Params.current()
        .get(PAGE_SIZE_PARAM, Long.class)).or(DEFAULT_PAGE_SIZE);
    return query.restrict(new QueryModifiers(limit,
        (page - 1L) * DEFAULT_PAGE_SIZE));
  }

  public static JPQLQuery<?> createPaginatedQuery() {
    return paginatedQuery(createQuery());
  }

  /**
   * @return a simplequery object, wrap list or listResults.
   */
  public static <T> SimpleResults<T> wrap(JPQLQuery<?> query,
      Expression<T> expression) {
    return new SimpleResults<T>(query, expression);
  }

  /**
   * meglio usare il factory per questo.
   *
   * @return jpaquery
   */
  @Deprecated
  public static JPAQuery<?> clone(JPAQuery<?> qry) {
    return qry.clone(JPA.em());
  }

  public static boolean isNotEmpty(BaseModel model) {
    return model != null && model.isPersistent();
  }

  /**
   * @return la funzione di trasformazione da modello a proprio id.
   */
  public static <T extends BaseModel> Function<T, Long> jpaId() {
    return input -> input.id;
  }

  /**
   * @return la funzione per ottenere un oggetto via em.find().
   */
  public static <T extends BaseModel> Function<Integer, T> jpaFind(final Class<T> model) {
    return id -> JPA.em().find(model, id);
  }

  /**
   * @author marco
   */
  public static class SimpleResults<T> {

    private final Expression<T> expression;
    private final JPQLQuery<?> query;

    SimpleResults(JPQLQuery<?> query, Expression<T> expression) {
      this.query = query;
      this.expression = expression;
    }

    public long count() {
      return query.fetchCount();
    }

    public List<T> list() {
      return (List<T>) query.fetch();
    }

    public List<T> list(long limits) {
      return (List<T>) query.restrict(QueryModifiers.limit(limits)).fetch();
    }

    public QueryResults<T> listResults() {
      return (QueryResults<T>) paginatedQuery(query).fetchResults();
    }

  }
}
