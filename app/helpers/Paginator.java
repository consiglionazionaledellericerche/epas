package helpers;

import java.util.Map;

import play.mvc.Http.Request;
import play.mvc.Router;
import play.mvc.Scope;
import play.mvc.Scope.Params;

import com.google.common.base.Preconditions;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import com.mysema.query.SearchResults;

/**
 * @author marco
 *
 */
public class Paginator {

	/**
	 * Nome della argomento da passare via request.
	 * Vedere anche helpers.ModelQuery.PAGE_SIZE_PARAM
	 */
	public static final String PAGE_PARAM = "page";

	public final boolean hasNext;
	public final boolean hasPrevious;
	private final int size;
	private final Map<String, Object> params;
	private final String action;
	public final int current;
	public final Iterable<Integer> pages;

	public Paginator(SearchResults<?> results) {
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

		int lastPage = (int) (results.getTotal() / results.getLimit() +
				(results.getTotal() % results.getLimit() == 0 ? 0 : 1));
		pages = ContiguousSet.create(Range.closed(Math.max(current - 5, 1), 
					Math.min(current + 5, lastPage)), 
					DiscreteDomain.integers());
	}

	public String getPrevious() {
		return urlFor(current -1 );
	}

	public String getNext() {
		return urlFor(current +1);
	}
	
	public String urlFor(int page) {
		Preconditions.checkArgument(page >= 0);
		params.put("page", Integer.toString(page));
		return Router.reverse(action, params).url;
	}

	public static Paginator instanceFor(SearchResults<?> sr) {
		return new Paginator(sr);
	}
	
	// too long uri methods
	
	public boolean isTooLong() {
		return urlFor(current).length() > 1024;
	}
	
	public String getSimpleUrl() {
		return Router.reverse(action).url;
	}
	
	public Object dataFor(int page) {
		Preconditions.checkArgument(page >= 0);
		params.put("page", Integer.toString(page));
		return params;
	}

	public Object getNextData() {
		return dataFor(current +1);
	}
	
	public Object getPreviousData() {
		return dataFor(current -1);
	}
}
