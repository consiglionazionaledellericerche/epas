package helpers.jpa;

import com.google.common.base.Function;
import com.google.common.base.Optional;

import com.mysema.query.QueryModifiers;
import com.mysema.query.SearchResults;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.impl.JPAQuery;
import com.mysema.query.types.Expression;

import helpers.Paginator;

import models.base.BaseModel;

import play.db.jpa.JPA;
import play.mvc.Scope;

import java.util.List;

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

  public static JPQLQuery createQuery() {
    return new JPAQuery(JPA.em());
  }

  public static JPQLQuery paginatedQuery(JPQLQuery query) {
    final Integer page = Optional.fromNullable(Scope.Params.current()
        .get(Paginator.PAGE_PARAM, Integer.class)).or(1);
    final long limit = Optional.fromNullable(Scope.Params.current()
        .get(PAGE_SIZE_PARAM, Long.class)).or(DEFAULT_PAGE_SIZE);
    return query.restrict(new QueryModifiers(limit,
        (page - 1L) * DEFAULT_PAGE_SIZE));
  }

  public static JPQLQuery createPaginatedQuery() {
    return paginatedQuery(createQuery());
  }

  /**
   * @return a simplequery object, wrap list or listResults.
   */
  public static <T> SimpleResults<T> wrap(JPQLQuery query,
      Expression<T> expression) {
    return new SimpleResults<T>(query, expression);
  }

  /**
   * meglio usare il factory per questo.
   *
   * @return jpaquery
   */
  @Deprecated
  public static JPAQuery clone(JPAQuery qry) {
    return qry.clone(JPA.em());
  }

  public static boolean isNotEmpty(BaseModel model) {
    return model != null && model.isPersistent();
  }

  /**
   * @return la funzione di trasformazione da modello a proprio id.
   */
  public static <T extends BaseModel> Function<T, Long> jpaId() {
    return new Function<T, Long>() {

      @Override
      public Long apply(T input) {
        return input.id;
      }
    };
  }

  /**
   * @return la funzione per ottenere un oggetto via em.find().
   */
  public static <T extends BaseModel> Function<Integer, T> jpaFind(final Class<T> model) {
    return new Function<Integer, T>() {

      @Override
      public T apply(Integer id) {
        return JPA.em().find(model, id);
      }
    };
  }

  /**
   * @author marco
   */
  public static class SimpleResults<T> {
    private final Expression<T> expression;
    private final JPQLQuery query;

    SimpleResults(JPQLQuery query, Expression<T> expression) {
      this.query = query;
      this.expression = expression;
    }

    public long count() {
      return query.count();
    }

    public List<T> list() {
      return query.list(expression);
    }

    public List<T> list(long limits) {
      return query.restrict(QueryModifiers.limit(limits)).list(expression);
    }

    public SearchResults<T> listResults() {
      return paginatedQuery(query).listResults(expression);
    }

  }
}
