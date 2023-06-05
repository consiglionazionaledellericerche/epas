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

package helpers;

import com.google.common.base.Preconditions;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import com.querydsl.core.QueryResults;
import java.util.Map;
import play.mvc.Http.Request;
import play.mvc.Router;
import play.mvc.Scope;
import play.mvc.Scope.Params;

/**
 * Paginatore.
 *
 * @author Marco Andreini
 */
public class Paginator {

  /**
   * Nome della argomento da passare via request. Vedere anche helpers.ModelQuery.PAGE_SIZE_PARAM
   */
  public static final String PAGE_PARAM = "page";

  public final boolean hasNext;
  public final boolean hasPrevious;
  public final int current;
  public final Iterable<Integer> pages;
  private final int size;
  private final Map<String, Object> params;
  private final String action;

  /**
   * Costruisce un paginatore a partire dai risultati di una query.
   *
   * @param results i risultati di una query.
   */
  public Paginator(QueryResults<?> results) {
    // this.results = results;
    size = results.getResults().size();
    hasNext = results.getTotal() > results.getOffset() + size;
    hasPrevious = results.getOffset() > 0;

    final Request request = Request.current();
    action = request != null ? request.action : null;

    Params currentParams = Scope.Params.current();
    params = Maps.newHashMap();
    if (currentParams != null) {
      Integer page = currentParams.get("page", Integer.class);
      current = page == null ? 1 : page;
      params.putAll(currentParams.allSimple());
    } else {
      current = 1;
    }
    params.remove("body");

    int lastPage = (int) (results.getTotal() / results.getLimit()
        + (results.getTotal() % results.getLimit() == 0 ? 0 : 1));
    pages = ContiguousSet.create(Range.closed(Math.max(current - 5, 1),
        Math.min(current + 5, lastPage)),
        DiscreteDomain.integers());
  }

  public static Paginator instanceFor(QueryResults<?> sr) {
    return new Paginator(sr);
  }

  public String getPrevious() {
    return urlFor(current - 1);
  }

  public String getNext() {
    return urlFor(current + 1);
  }

  /**
   * L'url della pagina.
   *
   * @param page il numero della pagina
   * @return la stringa per l'url della pagina
   */
  public String urlFor(int page) {
    Preconditions.checkArgument(page >= 0);
    params.put("page", Integer.toString(page));
    return Router.reverse(action, params).url;
  }

  // too long uri methods

  public boolean isTooLong() {
    return urlFor(current).length() > 1024;
  }

  public String getSimpleUrl() {
    return Router.reverse(action).url;
  }

  /**
   * Aggiorna i params col numero di pagina attuale.
   *
   * @param page il numero di pagina
   * @return l'oggetto params aggiunto delle info sul numero di pagina.
   */
  public Object dataFor(int page) {
    Preconditions.checkArgument(page >= 0);
    params.put("page", Integer.toString(page));
    return params;
  }

  public Object getNextData() {
    return dataFor(current + 1);
  }

  public Object getPreviousData() {
    return dataFor(current - 1);
  }
}
