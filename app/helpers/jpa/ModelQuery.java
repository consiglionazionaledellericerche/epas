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
 * Classe per le model query.
 *
 * @author Marco Andreini
 */
public class ModelQuery {

  /**
   * Nome dell'argomento da fornire via request per indicare il numero di elementi per pagina.
   */
  private static final String PAGE_SIZE_PARAM = "limit";
  private static final long DEFAULT_PAGE_SIZE = 10L;

  private ModelQuery() {
  }

  @SuppressWarnings("rawtypes")
  public static JPQLQuery<?> createQuery() {
    return new JPAQuery(JPA.em());
  }

  /**
   * Le query paginate.
   *
   * @param query la query da paginare
   * @return le query paginate.
   */
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
   * Il simpleresult che wrappa la lista o i listresults.
   *
   * @return a simplequery object, wrap list or listResults.
   */
  public static <T> SimpleResults<T> wrap(JPQLQuery<?> query,
      Expression<T> expression) {
    return new SimpleResults<T>(query);
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
   * La funzione di trasformazione da modello a id.
   *
   * @return la funzione di trasformazione da modello a proprio id.
   */
  public static <T extends BaseModel> Function<T, Long> jpaId() {
    return input -> input.id;
  }

  /**
   * Funzione di trasformazione da integer a modello.
   *
   * @return la funzione per ottenere un oggetto via em.find().
   */
  public static <T extends BaseModel> Function<Integer, T> jpaFind(final Class<T> model) {
    return id -> JPA.em().find(model, id);
  }

  /**
   * Classe simpleResult.
   *
   * @author Marco Andreini
   */
  public static class SimpleResults<T> {

    private final JPQLQuery<?> query;

    SimpleResults(JPQLQuery<?> query) {
      this.query = query;
    }

    public long count() {
      return query.fetchCount();
    }

    @SuppressWarnings("unchecked")
    public List<T> list() {
      return (List<T>) query.fetch();
    }

    @SuppressWarnings("unchecked")
    public List<T> list(long limits) {
      return (List<T>) query.restrict(QueryModifiers.limit(limits)).fetch();
    }

    @SuppressWarnings("unchecked")
    public QueryResults<T> listResults() {
      return (QueryResults<T>) paginatedQuery(query).fetchResults();
    }

  }
}
